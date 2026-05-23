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
import dev.stephyu.conversation.domain.ConversationTurn;
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
        var analysis = conversationAnalyzerPort.analyze(buildAnalysisRequest(session, message));
        String language = resolveLanguage(session.state(), analysis.language());
        ResponseTone responseTone = responseStyleResolver.resolve(session.state().establishmentId());
        ConversationSession localizedSession = session.withState(session.state().withLanguage(language));
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
                hasIntentNamed(analysis.intents(), AnalyzedIntentName.AFFIRMATIVE),
                hasIntentNamed(analysis.intents(), AnalyzedIntentName.NEGATIVE),
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
        LOGGER.debug(
                "Starting workflow from intents: sessionId={}, selectedWorkflow={}, entities={}",
                session.sessionId().value(),
                workflowSelection.orElseThrow().workflowType().name(),
                collectAllEntities(analysis.intents()));
        ReplyDirective directive = workflowProcessor.process(new HandlerInput(
                session,
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
        List<ConversationAnalysisRequest.CollectedValueSnapshot> collectedData = activeWorkflow
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
            case GREETING -> Optional.of(WorkflowType.GREETING);
            case THANKS -> Optional.of(WorkflowType.THANKS);
            case GOODBYE -> Optional.of(WorkflowType.GOODBYE);
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
