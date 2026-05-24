package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort.ReplyIntent;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDefinition;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WorkflowProcessor {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Europe/Paris");
    private static final Pattern NAME_PREFIX_PATTERN = Pattern.compile(
            "^(?:au\\s+nom\\s+de|name\\s+is|under\\s+the\\s+name|under|for)\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAUSIBLE_NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L}'\\- ]{0,79}$");
    private static final Pattern RESERVATION_INTENT_TEXT_PATTERN = Pattern.compile(
            ".*\\b(?:reserver|reserve|reservation|book|booking)\\b.*",
            Pattern.CASE_INSENSITIVE);

    private final SlotValueNormalizer slotValueNormalizer;

    public WorkflowProcessor(SlotValueNormalizer slotValueNormalizer) {
        this.slotValueNormalizer = Objects.requireNonNull(slotValueNormalizer, "slotValueNormalizer must not be null");
    }

    public ReplyDirective process(HandlerInput input, IntentHandler handler) {
        ConversationState state = input.session().state();
        Workflow workflow = state.activeWorkflow().orElseGet(handler::newWorkflow);
        EntityApplicationResult applicationResult = applyEntities(
                workflow.collectedData(),
                input.entities(),
                input.language(),
                input.message());
        Workflow updatedWorkflow = workflow.withCollectedData(applicationResult.collectedData());
        updatedWorkflow = inferMissingReservationName(updatedWorkflow, input.message());

        List<SlotDefinition> missingRequiredSlots = updatedWorkflow.missingRequiredSlots();
        if (!missingRequiredSlots.isEmpty()) {
            Map<String, String> arguments = buildSlotPromptArguments(updatedWorkflow, missingRequiredSlots);
            return new ReplyDirective(state.withWorkflow(updatedWorkflow), ReplyIntent.ASK_SLOT, arguments);
        }

        boolean hasEntityUpdates = applicationResult.appliedUpdates();
        if (input.affirmative() && !hasEntityUpdates) {
            return confirm(input, handler, state, updatedWorkflow);
        }

        if (input.negative() && !hasEntityUpdates) {
            return new ReplyDirective(
                    state.withWorkflow(updatedWorkflow),
                    ReplyIntent.ASK_MODIFICATION,
                    Map.of());
        }

        if (handler.skipConfirmation()) {
            return confirm(input, handler, state, updatedWorkflow);
        }

        return new ReplyDirective(
                state.withWorkflow(updatedWorkflow),
                ReplyIntent.ASK_CONFIRMATION,
                handler.confirmationArguments(updatedWorkflow));
    }

    private ReplyDirective confirm(
            HandlerInput input,
            IntentHandler handler,
            ConversationState state,
            Workflow workflow) {
        WorkflowPostProcessResult result = handler.onConfirmed(input, workflow);
        // Clear the workflow on both success and failure when confirmation is skipped,
        // so the user is never stuck in a failed workflow state.
        ConversationState nextState = (result.success() || handler.skipConfirmation())
                ? state.withoutWorkflow()
                : state.withWorkflow(workflow);
        if (result.success() && result.reservationReference() != null) {
            nextState = nextState.withReservationReference(result.reservationReference());
        }
        return new ReplyDirective(nextState, result.success() ? ReplyIntent.WORKFLOW_SUCCESS : ReplyIntent.WORKFLOW_FAILURE, result.arguments(), result.directReply());
    }

    private EntityApplicationResult applyEntities(
            CollectedData collectedData,
            List<AnalyzedEntity> entities,
            String language,
            String rawMessage) {
        CollectedData updatedData = collectedData;
        boolean appliedUpdates = false;
        SlotNormalizationContext context = new SlotNormalizationContext(
                language,
                DEFAULT_ZONE_ID,
                LocalDate.now(DEFAULT_ZONE_ID));
        for (AnalyzedEntity entity : entities) {
            var slotName = SlotName.fromEntityType(entity.type());
            if (slotName.isEmpty()) {
                continue;
            }
            var normalizedValue = slotValueNormalizer.normalize(entity, slotName.orElseThrow(), context);
            if (normalizedValue.isEmpty()) {
                continue;
            }
            if (shouldIgnoreEntity(entity, slotName.orElseThrow(), normalizedValue.orElseThrow(), updatedData, rawMessage)) {
                continue;
            }
            updatedData = updatedData.withValue(slotName.orElseThrow(), normalizedValue.orElseThrow());
            appliedUpdates = true;
        }
        return new EntityApplicationResult(updatedData, appliedUpdates);
    }

    private static boolean shouldIgnoreEntity(
            AnalyzedEntity entity,
            SlotName slotName,
            SlotDataValue normalizedValue,
            CollectedData collectedData,
            String rawMessage) {
        if (slotName.equals(SlotName.RESERVATION_NAME) && !isPlausibleReservationName(entity.rawValue())) {
            return true;
        }
        Optional<SlotDataValue> existingValue = collectedData.value(slotName);
        if (existingValue.isEmpty()) {
            return false;
        }
        String candidate = formatSlotValue(normalizedValue);
        if (!formatSlotValue(existingValue.orElseThrow()).equalsIgnoreCase(candidate)) {
            return false;
        }
        String rawCandidate = entity.rawValue().toLowerCase(Locale.ROOT);
        return !rawMessage.toLowerCase(Locale.ROOT).contains(rawCandidate);
    }

    private static Workflow inferMissingReservationName(Workflow workflow, String rawMessage) {
        if (workflow.collectedData().contains(SlotName.RESERVATION_NAME)) {
            return workflow;
        }
        Optional<String> inferredName = inferReservationName(rawMessage);
        if (inferredName.isEmpty()) {
            return workflow;
        }
        return workflow.withCollectedData(
                workflow.collectedData().withValue(
                        SlotName.RESERVATION_NAME,
                        new SlotDataValue.TextValue(inferredName.orElseThrow())));
    }

    private static Optional<String> inferReservationName(String rawMessage) {
        String trimmed = rawMessage.trim();
        if (trimmed.isBlank()) {
            return Optional.empty();
        }
        var matcher = NAME_PREFIX_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String candidate = matcher.replaceFirst("").trim();
        if (!isPlausibleReservationName(candidate)) {
            return Optional.empty();
        }
        String normalized = candidate.replaceAll("\\s+", " ");
        if (normalized.split(" ").length > 4) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    private static boolean isPlausibleReservationName(String candidate) {
        String normalized = candidate.trim();
        return PLAUSIBLE_NAME_PATTERN.matcher(normalized).matches()
                && !RESERVATION_INTENT_TEXT_PATTERN.matcher(normalized).matches();
    }

    private static String formatSlotValue(SlotDataValue value) {
        return switch (value) {
            case SlotDataValue.TextValue(String text) -> text;
            case SlotDataValue.DateValue(LocalDate date) -> date.toString();
            case SlotDataValue.TimeValue(LocalTime time) -> time.toString();
            case SlotDataValue.NumberValue(int number) -> Integer.toString(number);
            case SlotDataValue.BooleanValue(boolean flag) -> Boolean.toString(flag);
        };
    }


    private record EntityApplicationResult(CollectedData collectedData, boolean appliedUpdates) {
    }

    // Human-readable labels for each slot — sent to the Reply LLM so it asks the right question.
    private static final Map<String, String> SLOT_LABELS = Map.of(
            "reservation_name", "the customer name for the booking",
            "date", "the date of the reservation",
            "time", "the time of the reservation",
            "people_count", "the number of guests",
            "reference_number", "the booking reference number"
    );

    private static Map<String, String> buildSlotPromptArguments(
            Workflow workflow,
            List<SlotDefinition> missingRequiredSlots) {
        Map<String, String> args = new java.util.LinkedHashMap<>();
        // All remaining slots in order — the LLM must ask only for the first one.
        String slotsToCollect = missingRequiredSlots.stream()
                .map(s -> SLOT_LABELS.getOrDefault(s.name().value(), s.name().value()))
                .reduce((a, b) -> a + " > " + b)
                .orElse("");
        args.put("slots_to_collect", slotsToCollect);
        // Include already collected data so the LLM can acknowledge them naturally.
        Map<dev.stephyu.conversation.domain.slot.SlotName, dev.stephyu.conversation.domain.slot.SlotDataValue> collected =
                workflow.collectedData().values();
        if (!collected.isEmpty()) {
            String summary = collected.entrySet().stream()
                    .map(e -> e.getKey().value() + "=" + formatSlotValue(e.getValue()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            args.put("already_collected", summary);
        }
        return Map.copyOf(args);
    }
}
