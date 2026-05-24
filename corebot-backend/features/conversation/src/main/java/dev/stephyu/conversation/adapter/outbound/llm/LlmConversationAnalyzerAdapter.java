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

        List<AnalyzedIntent> intents = buildIntents(payload);
        ConversationAnalysis analysis = new ConversationAnalysis(
                detectedLanguage, intents, payload.isAffirmative(), payload.isNegative());
        LOGGER.debug("LLM analysis output: {}", formatOutputForLog(analysis));
        return analysis;
    }

    private static List<AnalyzedIntent> buildIntents(ConversationAnalyzerLlm.ConversationAnalysisPayload payload) {
        AnalyzedIntentName mainIntent = payload.mainIntent() == null ? AnalyzedIntentName.UNKNOWN : payload.mainIntent();
        List<AnalyzedEntity> entities = buildEntities(mainIntent, payload.reservationDetails());
        return List.of(new AnalyzedIntent(mainIntent, entities));
    }

    private static List<AnalyzedEntity> buildEntities(
            AnalyzedIntentName mainIntent,
            ConversationAnalyzerLlm.@Nullable ReservationDetailsPayload details) {
        if (details == null) {
            return List.of();
        }
        List<AnalyzedEntity> entities = new ArrayList<>();
        if (mainIntent == AnalyzedIntentName.RESERVATION_CREATE) {
            addEntity(entities, AnalyzedEntityType.RESERVATION_NAME, details.customerName());
            addEntity(entities, AnalyzedEntityType.PEOPLE_COUNT, details.peopleCount());
            addEntity(entities, AnalyzedEntityType.DATE, details.date());
            addEntity(entities, AnalyzedEntityType.TIME, details.time());
        } else if (mainIntent == AnalyzedIntentName.RESERVATION_CHECK
                || mainIntent == AnalyzedIntentName.RESERVATION_CANCEL) {
            addEntity(entities, AnalyzedEntityType.REFERENCE_NUMBER, details.referenceNumber());
        }
        return List.copyOf(entities);
    }

    private static void addEntity(List<AnalyzedEntity> entities, AnalyzedEntityType type, @org.jspecify.annotations.Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank() || rawValue.equalsIgnoreCase("null") || rawValue.equals("0")) {
            if (rawValue != null && !rawValue.isBlank()) {
                LOGGER.debug("Ignoring LLM sentinel value for entity: type={}, rawValue={}", type, rawValue);
            }
            return;
        }
        entities.add(new AnalyzedEntity(type, rawValue));
    }

    // ── formatting helpers ────────────────────────────────────────────────────

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
                + "\n  affirmative=" + analysis.affirmative()
                + "\n  negative=" + analysis.negative()
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

    @SuppressWarnings("NullAway")
    private static String normalizeDetectedLanguage(String language) {
        if (language.isBlank() || language.equalsIgnoreCase("unknown")) {
            return null; // NOSONAR intentionally null for fallback
        }
        return language.toLowerCase(java.util.Locale.ROOT);
    }
}
