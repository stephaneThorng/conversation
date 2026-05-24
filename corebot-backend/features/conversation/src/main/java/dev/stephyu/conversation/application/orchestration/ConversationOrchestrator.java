package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysisRequest;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import dev.stephyu.conversation.application.reply.ConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.EstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class ConversationOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationOrchestrator.class);
    private static final String FALLBACK_LANGUAGE = "en";
    private static final String DEFAULT_TIMEZONE = "Europe/Paris";

    private final ConversationAnalyzerPort conversationAnalyzerPort;
    private final IntentHandlerRegistry intentHandlerRegistry;
    private final ConversationReplyCatalog replyCatalog;
    private final EstablishmentResponseStyleResolver responseStyleResolver;
    private final WorkflowProcessor workflowProcessor;
    private final WorkflowReplyResolver workflowReplyResolver;

    public ConversationOrchestrator(
            ConversationAnalyzerPort conversationAnalyzerPort,
            IntentHandlerRegistry intentHandlerRegistry,
            ConversationReplyCatalog replyCatalog,
            EstablishmentResponseStyleResolver responseStyleResolver,
            WorkflowProcessor workflowProcessor,
            WorkflowReplyResolver workflowReplyResolver) {
        this.conversationAnalyzerPort = conversationAnalyzerPort;
        this.intentHandlerRegistry = intentHandlerRegistry;
        this.replyCatalog = replyCatalog;
        this.responseStyleResolver = responseStyleResolver;
        this.workflowProcessor = workflowProcessor;
        this.workflowReplyResolver = workflowReplyResolver;
    }

    public OrchestrationResult orchestrate(ConversationSession session, String message) {

        // LLM analysis
        var analysis = conversationAnalyzerPort.analyze(buildAnalysisRequest(session, message));

        // Detect language for the conversation, prioritizing LLM detection, then session state, then fallback
        String language = resolveLanguage(session.state(), analysis.language());
        ConversationSession localizedSession = session.withState(session.state().withLanguage(language));

        ResponseTone responseTone = responseStyleResolver.resolve(session.state().establishmentId());
        LOGGER.debug(
                "Orchestrating conversation: sessionId={}, activeWorkflow={}, detectedLanguage={}, intents={}, recentTurnsCount={}, missingRequiredSlots={}",
                session.sessionId().value(),
                session.state().activeWorkflow().map(Workflow::type).map(Enum::name).orElse("none"),
                language,
                formatIntentNames(analysis.intents()),
                session.state().recentTurns().size(),
                describeMissingRequiredSlots(session.state()));

        if (hasIntentNamed(analysis.intents(), AnalyzedIntentName.CANCEL)) {
            String reply = replyCatalog.resolve(language, responseTone, "workflow.cancelled", Map.of());
            OrchestrationResult result = clearWorkflowWithReply(localizedSession, reply);
            LOGGER.debug("Conversation reply resolved: sessionId={}, reply={}", session.sessionId().value(), result.reply());
            return result;
        }

        if (localizedSession.state().hasActiveWorkflow()) {
            // If the detected intent maps to a different workflow type than the active one,
            // or to the same workflow type but with a fresh intent (no leftover data needed),
            // abandon the current workflow and start fresh.
            Workflow activeWorkflow = localizedSession.state().activeWorkflow().orElseThrow();
            Optional<WorkflowType> detectedWorkflowType = analysis.intents().stream()
                    .map(intent -> mapWorkflowType(intent.name()))
                    .flatMap(Optional::stream)
                    .findFirst();
            boolean shouldSwitchWorkflow = detectedWorkflowType.isPresent()
                    && detectedWorkflowType.orElseThrow() != activeWorkflow.type();
            if (shouldSwitchWorkflow) {
                ConversationSession clearedSession = localizedSession.withState(localizedSession.state().withoutWorkflow());
                OrchestrationResult result = handleWithoutActiveWorkflow(clearedSession, message, analysis, language, responseTone);
                LOGGER.debug("Conversation reply resolved: sessionId={}, reply={}", session.sessionId().value(), result.reply());
                return result;
            }
            OrchestrationResult result = handleActiveWorkflow(localizedSession, message, analysis, language, responseTone);
            LOGGER.debug("Conversation reply resolved: sessionId={}, reply={}", session.sessionId().value(), result.reply());
            return result;
        }

        OrchestrationResult result = handleWithoutActiveWorkflow(localizedSession, message, analysis, language, responseTone);
        LOGGER.debug("Conversation reply resolved: sessionId={}, reply={}", session.sessionId().value(), result.reply());
        return result;
    }

    private OrchestrationResult handleActiveWorkflow(
            ConversationSession session,
            String message,
            dev.stephyu.conversation.application.analysis.ConversationAnalysis analysis,
            String language,
            ResponseTone responseTone) {
        Workflow workflow = session.state().activeWorkflow().orElseThrow();
        IntentHandler handler = intentHandlerRegistry.get(workflow.type()).orElseThrow();
        LOGGER.debug(
                "Handling active workflow: sessionId={}, workflowType={}, collectedData={}, entities={}",
                session.sessionId().value(),
                workflow.type().name(),
                describeCollectedData(workflow),
                collectAllEntities(analysis.intents()));

        ReplyDirective directive = workflowProcessor.process(new HandlerInput(
                session,
                message,
                collectAllEntities(analysis.intents()),
                analysis.affirmative(),
                analysis.negative(),
                language,
                responseTone), handler);
        HandlerResult result = workflowReplyResolver.resolve(directive, language, responseTone);
        return withReply(session, result.state(), result.reply());
    }

    private OrchestrationResult handleWithoutActiveWorkflow(
            ConversationSession session,
            String message,
            dev.stephyu.conversation.application.analysis.ConversationAnalysis analysis,
            String language,
            ResponseTone responseTone) {
        Optional<WorkflowSelection> workflowSelection = selectWorkflow(analysis.intents());
        if (workflowSelection.isEmpty()) {
            String reply = replyCatalog.resolve(language, responseTone, "workflow.not_understood", Map.of());
            return withReply(session, session.state(), reply);
        }

        IntentHandler handler = workflowSelection.orElseThrow().handler();

        // For RESERVATION_CHECK / RESERVATION_CANCEL: pre-fill reference from session state if available
        ConversationSession sessionForWorkflow = session;
        if (workflowSelection.orElseThrow().workflowType() == WorkflowType.RESERVATION_CHECK
                && handler instanceof ReservationCheckHandler checkHandler
                && session.state().reservationReference().isPresent()) {
            String ref = session.state().reservationReference().orElseThrow();
            Workflow prefilledWorkflow = checkHandler.newWorkflowWithReference(ref);
            sessionForWorkflow = session.withState(session.state().withWorkflow(prefilledWorkflow));
        } else if (workflowSelection.orElseThrow().workflowType() == WorkflowType.RESERVATION_CANCEL
                && handler instanceof ReservationCancelHandler cancelHandler
                && session.state().reservationReference().isPresent()) {
            String ref = session.state().reservationReference().orElseThrow();
            Workflow prefilledWorkflow = cancelHandler.newWorkflowWithReference(ref);
            sessionForWorkflow = session.withState(session.state().withWorkflow(prefilledWorkflow));
        }

        LOGGER.debug(
                "Starting workflow from intents: sessionId={}, selectedWorkflow={}, entities={}",
                session.sessionId().value(),
                workflowSelection.orElseThrow().workflowType().name(),
                collectAllEntities(analysis.intents()));
        ReplyDirective directive = workflowProcessor.process(new HandlerInput(
                sessionForWorkflow,
                message,
                collectAllEntities(analysis.intents()),
                false,
                false,
                language,
                responseTone), handler);
        HandlerResult result = workflowReplyResolver.resolve(directive, language, responseTone);
        return withReply(session, result.state(), result.reply());
    }

    private ConversationAnalysisRequest buildAnalysisRequest(ConversationSession session, String message) {
        ConversationState state = session.state();
        Optional<Workflow> activeWorkflow = state.activeWorkflow();

        var collectedData = activeWorkflow
                .map(workflow -> workflow.collectedData().values().entrySet().stream()
                        .map(entry -> new ConversationAnalysisRequest.CollectedValueSnapshot(
                                entry.getKey().value(),
                                entry.getValue().type().name(),
                                formatSlotValue(entry.getValue())))
                        .toList())
                .orElse(List.of());

        List<String> missingRequiredSlots = activeWorkflow
                .map(workflow -> workflow.missingRequiredSlots().stream()
                        .map(slot -> slot.name().value())
                        .toList())
                .orElse(List.of());

        return new ConversationAnalysisRequest(
                message,
                session.sessionId(),
                state.establishmentId(),
                activeWorkflow.map(Workflow::type).orElse(null),
                collectedData,
                missingRequiredSlots,
                activeWorkflow.map(Workflow::isAwaitingConfirmation).orElse(false),
                state.recentTurns().stream()
                        .map(turn -> new ConversationAnalysisRequest.ConversationTurnSnapshot(turn.role(), turn.content()))
                        .toList(),
                state.language(),
                DEFAULT_TIMEZONE,
                LocalDate.now(java.time.ZoneId.of(DEFAULT_TIMEZONE)));
    }

    private static List<dev.stephyu.conversation.application.analysis.AnalyzedEntity> collectAllEntities(
            List<dev.stephyu.conversation.application.analysis.AnalyzedIntent> intents) {
        List<dev.stephyu.conversation.application.analysis.AnalyzedEntity> entities = new ArrayList<>();
        for (var intent : intents) {
            entities.addAll(intent.entities());
        }
        return entities;
    }

    private static boolean hasIntentNamed(List<AnalyzedIntent> intents, AnalyzedIntentName intentName) {
        return intents.stream().anyMatch(intent -> intent.name() == intentName);
    }

    private Optional<WorkflowSelection> selectWorkflow(List<AnalyzedIntent> intents) {
        for (AnalyzedIntent intent : intents) {
            Optional<WorkflowType> workflowType = mapWorkflowType(intent.name());
            if (workflowType.isEmpty()) {
                continue;
            }
            Optional<IntentHandler> handler = intentHandlerRegistry.get(workflowType.orElseThrow());
            if (handler.isPresent()) {
                return Optional.of(new WorkflowSelection(workflowType.orElseThrow(), handler.orElseThrow()));
            }
            LOGGER.debug("No handler registered for workflow type {}", workflowType.orElseThrow());
        }
        return Optional.empty();
    }

    private static Optional<WorkflowType> mapWorkflowType(AnalyzedIntentName intentName) {
        return switch (intentName) {
            case RESERVATION_CREATE -> Optional.of(WorkflowType.RESERVATION_CREATE);
            case RESERVATION_CHECK -> Optional.of(WorkflowType.RESERVATION_CHECK);
            case RESERVATION_CANCEL -> Optional.of(WorkflowType.RESERVATION_CANCEL);
            case ASK_MENU -> Optional.of(WorkflowType.ASK_MENU);
            case ASK_MENU_ITEM -> Optional.of(WorkflowType.ASK_MENU_ITEM);
            default -> Optional.empty();
        };
    }

    private static String resolveLanguage(
            ConversationState state,
            @Nullable String detectedLanguage) {
        if (detectedLanguage != null && !detectedLanguage.isBlank()) {
            return detectedLanguage.toLowerCase(Locale.ROOT);
        }
        if (state.language() != null && !state.language().isBlank()) {
            return state.language();
        }
        return FALLBACK_LANGUAGE;
    }

    private static String formatSlotValue(SlotDataValue value) {
        return switch (value) {
            case SlotDataValue.TextValue(String text) -> text;
            case SlotDataValue.DateValue(java.time.LocalDate date) -> date.toString();
            case SlotDataValue.TimeValue(java.time.LocalTime time) -> time.toString();
            case SlotDataValue.NumberValue(int number) -> Integer.toString(number);
            case SlotDataValue.BooleanValue(boolean flag) -> Boolean.toString(flag);
        };
    }

    private static String formatIntentNames(List<AnalyzedIntent> intents) {
        if (intents.isEmpty()) {
            return "none";
        }
        return intents.stream()
                .map(intent -> intent.name().name())
                .reduce((left, right) -> left + "," + right)
                .orElse("none");
    }

    private static String describeMissingRequiredSlots(ConversationState state) {
        return state.activeWorkflow()
                .map(workflow -> workflow.missingRequiredSlots().stream()
                        .map(slot -> slot.name().value())
                        .reduce((left, right) -> left + "," + right)
                        .orElse("none"))
                .orElse("none");
    }

    private static String describeCollectedData(Workflow workflow) {
        if (workflow.collectedData().values().isEmpty()) {
            return "none";
        }
        return workflow.collectedData().values().entrySet().stream()
                .map(entry -> entry.getKey().value() + "=" + formatSlotValue(entry.getValue()))
                .reduce((left, right) -> left + "," + right)
                .orElse("none");
    }

    private static OrchestrationResult withReply(ConversationSession session, ConversationState state, String reply) {
        return new OrchestrationResult(session.withState(state.withLastAssistantReply(reply)), reply);
    }

    private static OrchestrationResult clearWorkflowWithReply(ConversationSession session, String reply) {
        return withReply(session, session.state().withoutWorkflow(), reply);
    }

    public record OrchestrationResult(ConversationSession session, String reply) {
    }

    private record WorkflowSelection(WorkflowType workflowType, IntentHandler handler) {
    }
}
