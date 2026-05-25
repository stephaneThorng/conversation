package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.analysis.ConversationAnalysisRequest;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class LlmConversationAnalyzerAdapter implements ConversationAnalyzerPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmConversationAnalyzerAdapter.class);

    private final ConversationAnalyzerLlm analyzer;

    public LlmConversationAnalyzerAdapter(ConversationAnalyzerLlm analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public ConversationAnalysis analyze(ConversationAnalysisRequest request) {
        LOGGER.debug("LLM analyzer input: {}", formatInputForLog(request));
        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = analyzer.analyze(
                request.sessionId().value(),
                request.message(),
                buildAnalysisHints(request),
                formatRecentTurns(request.recentTurns()),
                formatWorkflowType(request.activeWorkflowType()),
                formatCollectedData(request.collectedData()),
                formatMissingRequiredSlots(request.missingRequiredSlots()),
                request.awaitingConfirmation(),
                nullableText(request.language()));

        String detectedLanguage = normalizeDetectedLanguage(payload.language());
        List<AnalyzedIntent> intents = buildIntents(payload, request.message());
        ConversationAnalysis analysis = new ConversationAnalysis(
                detectedLanguage, intents, payload.isAffirmative(), payload.isNegative());
        LOGGER.debug("LLM analyzer output: {}", formatOutputForLog(payload, analysis));
        return analysis;
    }

    private static List<AnalyzedIntent> buildIntents(ConversationAnalyzerLlm.ConversationAnalysisPayload payload, String message) {
        AnalyzedIntentName mainIntent = payload.mainIntent() == null ? AnalyzedIntentName.UNKNOWN : payload.mainIntent();
        List<AnalyzedEntity> entities = buildEntities(mainIntent, payload.reservationDetails(), message);
        return List.of(new AnalyzedIntent(mainIntent, entities));
    }

    private static List<AnalyzedEntity> buildEntities(
            AnalyzedIntentName mainIntent,
            ConversationAnalyzerLlm.@Nullable ReservationDetailsPayload details,
            String message) {
        List<AnalyzedEntity> entities = new ArrayList<>();
        if (mainIntent == AnalyzedIntentName.RESERVATION_CREATE && details != null) {
            addEntity(entities, AnalyzedEntityType.RESERVATION_NAME, details.customerName(), message);
            addEntity(entities, AnalyzedEntityType.PEOPLE_COUNT, details.peopleCount(), message);
            addEntity(entities, AnalyzedEntityType.DATE, details.date(), message);
            addEntity(entities, AnalyzedEntityType.TIME, details.time(), message);
        } else if (mainIntent == AnalyzedIntentName.RESERVATION_CHECK
                || mainIntent == AnalyzedIntentName.RESERVATION_CANCEL) {
            if (details == null) {
                return List.copyOf(entities);
            }
            addEntity(entities, AnalyzedEntityType.REFERENCE_NUMBER, details.referenceNumber(), message);
        }
        return List.copyOf(entities);
    }

    private static void addEntity(
            List<AnalyzedEntity> entities,
            AnalyzedEntityType type,
            @org.jspecify.annotations.Nullable String rawValue,
            String message) {
        if (rawValue == null || rawValue.isBlank() || rawValue.equalsIgnoreCase("null") || rawValue.equals("0")) {
            if (rawValue != null && !rawValue.isBlank()) {
                LOGGER.debug("Ignoring LLM sentinel value for entity: type={}, rawValue={}", type, rawValue);
            }
            return;
        }
        if (!containsIgnoringCase(message, rawValue)) {
            LOGGER.debug("Ignoring LLM value absent from latest message: type={}, rawValue={}", type, rawValue);
            return;
        }
        entities.add(new AnalyzedEntity(type, rawValue));
    }

    private static boolean containsIgnoringCase(String text, String value) {
        return text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    // Assistant replies can be very long (lists of dishes). Truncate them to avoid
    // overflowing the LLM context window and triggering hallucinated output.
    private static final int MAX_ASSISTANT_TURN_CHARS = 300;

    // ── formatting helpers ────────────────────────────────────────────────────

    private static String formatInputForLog(ConversationAnalysisRequest request) {
        return "message=" + quote(request.message())
                + ", workflow=" + formatWorkflowType(request.activeWorkflowType())
                + ", missingSlots=" + formatMissingRequiredSlots(request.missingRequiredSlots())
                + ", awaitingConfirmation=" + request.awaitingConfirmation()
                + ", collectedData=" + formatCollectedData(request.collectedData())
                + ", languageHint=" + quote(nullableText(request.language()));
    }

    private static String formatOutputForLog(
            ConversationAnalyzerLlm.ConversationAnalysisPayload payload,
            ConversationAnalysis analysis) {
        return "language=" + nullableText(analysis.language())
                + ", rawLanguage=" + quote(payload.language())
                + ", mainIntent=" + analysis.firstIntent().map(intent -> intent.name().name()).orElse("UNKNOWN")
                + ", affirmative=" + analysis.affirmative()
                + ", negative=" + analysis.negative()
                + ", isCancel=" + payload.isCancel()
                + ", entities=" + formatEntities(analysis);
    }

    private static String formatEntities(ConversationAnalysis analysis) {
        return analysis.intents().stream()
                .flatMap(intent -> intent.entities().stream())
                .map(entity -> entity.type().name() + "=" + quote(entity.rawValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String formatWorkflowType(@Nullable Enum<?> workflowType) {
        return workflowType == null ? "none" : workflowType.name();
    }

    private static String buildAnalysisHints(ConversationAnalysisRequest request) {
        String activeWorkflowType = formatWorkflowType(request.activeWorkflowType());
        String missingRequiredSlots = formatMissingRequiredSlots(request.missingRequiredSlots());
        StringBuilder hints = new StringBuilder();
        if (request.activeWorkflowType() == null) {
            hints.append("No active workflow. Classify the latest message. ");
            hints.append("Extract reservation details only if explicitly present in the latest message. ");
            hints.append("Use RESERVATION_CANCEL only if the user explicitly refers to an existing reservation. ");
            hints.append("A bare cancel, annuler, or stop is ambiguous and should prefer UNKNOWN. ");
        } else {
            hints.append("Active workflow: ").append(activeWorkflowType).append(". ");
            hints.append("Missing slots: ").append(missingRequiredSlots).append(". ");
            hints.append("For short or ambiguous answers, use the active workflow as mainIntent. ");
            hints.append("A bare cancel, annuler, or stop means CANCEL for the current workflow. ");
        }
        if (request.awaitingConfirmation()) {
            hints.append("Awaiting confirmation. If the user only confirms, return UNKNOWN with isAffirmative=true. ");
            hints.append("If the user provides a corrected slot value, return the active reservation intent and extract only that corrected value. ");
        }
        if (activeWorkflowType.equals("RESERVATION_CHECK") || activeWorkflowType.equals("RESERVATION_CANCEL")) {
            hints.append("If the latest message contains an alphanumeric reservation code, extract it as referenceNumber. ");
        }
        hints.append("For menu questions, only return ASK_MENU or ASK_MENU_ITEM; do not extract menu filters. ");
        hints.append("Do not reuse collectedData or history as extracted values.");
        return hints.toString();
    }

    private static String formatCollectedData(List<ConversationAnalysisRequest.CollectedValueSnapshot> collectedData) {
        if (collectedData.isEmpty()) {
            return "none";
        }
        return collectedData.stream()
                .map(value -> value.slot() + ":" + value.type() + "=" + quote(value.value()))
                .collect(Collectors.joining(", "));
    }

    private static String formatMissingRequiredSlots(List<String> missingRequiredSlots) {
        if (missingRequiredSlots.isEmpty()) {
            return "none";
        }
        return String.join(", ", missingRequiredSlots);
    }

    private static String formatRecentTurns(List<ConversationAnalysisRequest.ConversationTurnSnapshot> recentTurns) {
        if (recentTurns.isEmpty()) {
            return "none";
        }
        return recentTurns.stream()
                .map(turn -> {
                    String content = turn.content();
                    // Truncate long assistant replies to avoid saturating the LLM context.
                    if (turn.role().name().equals("ASSISTANT") && content.length() > MAX_ASSISTANT_TURN_CHARS) {
                        content = content.substring(0, MAX_ASSISTANT_TURN_CHARS) + "…[tronqué]";
                    }
                    return turn.role().name() + ": " + quote(content);
                })
                .collect(Collectors.joining(" | "));
    }

    private static String nullableText(@Nullable String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    @SuppressWarnings("NullAway")
    private static String normalizeDetectedLanguage(String language) {
        if (language.isBlank() || language.equalsIgnoreCase("unknown")) {
            return null; // NOSONAR intentionally null for fallback
        }
        return language.toLowerCase(java.util.Locale.ROOT);
    }
}
