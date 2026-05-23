package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.analysis.ConversationAnalysisRequest;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import java.util.List;
import java.util.Optional;
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
        LOGGER.debug("LLM analysis input: {}", formatInputForLog(request));
        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = analyzer.analyze(
                request.sessionId().value(),
                request.message(),
                formatRecentTurns(request.recentTurns()),
                formatWorkflowType(request.activeWorkflowType()),
                formatCollectedData(request.collectedData()),
                formatMissingRequiredSlots(request.missingRequiredSlots()),
                request.awaitingConfirmation(),
                nullableText(request.language()));
        String detectedLanguage = normalizeDetectedLanguage(payload.language());
        if (detectedLanguage == null) {
            LOGGER.debug("LLM language not confidently detected, rawLanguage={}", payload.language());
        }
        List<AnalyzedIntent> intents = payload.intents().stream()
                .map(this::mapIntent)
                .toList();
        ConversationAnalysis analysis = new ConversationAnalysis(
                detectedLanguage,
                intents);
        LOGGER.debug("LLM analysis output: {}", formatOutputForLog(analysis));
        return analysis;
    }

    private AnalyzedIntent mapIntent(ConversationAnalyzerLlm.IntentPayload payload) {
        AnalyzedIntentName intentName = payload.name() == null ? AnalyzedIntentName.UNKNOWN : payload.name();
        if (intentName == AnalyzedIntentName.UNKNOWN) {
            LOGGER.debug("Unknown LLM intent received: {}", payload.name());
        }
        return new AnalyzedIntent(
                intentName,
                payload.entities().stream()
                        .map(this::mapEntity)
                        .flatMap(Optional::stream)
                        .toList());
    }

    private Optional<AnalyzedEntity> mapEntity(ConversationAnalyzerLlm.EntityPayload payload) {
        AnalyzedEntityType entityType = payload.type() == null ? AnalyzedEntityType.UNKNOWN : payload.type();
        if (entityType == AnalyzedEntityType.UNKNOWN) {
            LOGGER.debug("Unknown LLM entity type received: {}", payload.type());
        }
        if (payload.raw_value() == null || payload.raw_value().isBlank()) {
            LOGGER.debug(
                    "Ignoring LLM entity without raw_value: type={}",
                    payload.type());
            return Optional.empty();
        }
        return Optional.of(new AnalyzedEntity(entityType, payload.raw_value()));
    }

    private static String formatInputForLog(ConversationAnalysisRequest request) {
        return "\n  message=" + quote(request.message())
                + "\n  recentTurns=" + formatRecentTurns(request.recentTurns())
                + "\n  sessionId=" + request.sessionId().value()
                + "\n  establishmentId=" + request.establishmentId().value()
                + "\n  activeWorkflowType=" + formatWorkflowType(request.activeWorkflowType())
                + "\n  collectedData=" + formatCollectedData(request.collectedData())
                + "\n  missingRequiredSlots=" + formatMissingRequiredSlots(request.missingRequiredSlots())
                + "\n  awaitingConfirmation=" + request.awaitingConfirmation()
                + "\n  language=" + quote(nullableText(request.language()));
    }

    private static String formatOutputForLog(ConversationAnalysis analysis) {
        return "\n  language=" + analysis.language()
                + "\n  intentsCount=" + analysis.intents().size()
                + "\n  intents=" + analysis.intents();
    }

    private static String formatWorkflowType(@Nullable Enum<?> workflowType) {
        return workflowType == null ? "none" : workflowType.name();
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
                .map(turn -> turn.role().name() + ": " + quote(turn.content()))
                .collect(Collectors.joining(" | "));
    }

    private static String nullableText(@Nullable String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static @Nullable String normalizeDetectedLanguage(String language) {
        if (language.isBlank() || language.equalsIgnoreCase("unknown")) {
            return null;
        }
        return language.toLowerCase(java.util.Locale.ROOT);
    }
}
