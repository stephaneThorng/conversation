package dev.stephyu.conversation.adapter.outbound.llm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.output.structured.Description;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysisRequest;
import dev.stephyu.conversation.domain.ConversationTurn;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmConversationAnalyzerAdapterTest {

    @Test
    void mapsKnownValuesToEnums() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "en",
                        List.of(new ConversationAnalyzerLlm.IntentPayload(
                                AnalyzedIntentName.RESERVATION_CREATE,
                                List.of(new ConversationAnalyzerLlm.EntityPayload(
                                        AnalyzedEntityType.RESERVATION_NAME,
                                        "Martin")))))));

        var analysis = adapter.analyze(request());

        assertEquals(AnalyzedIntentName.RESERVATION_CREATE, analysis.firstIntent().orElseThrow().name());
        assertEquals(AnalyzedEntityType.RESERVATION_NAME, analysis.firstIntent().orElseThrow().entities().getFirst().type());
    }

    @Test
    void passesStructuredFieldsToAnalyzer() {
        RecordingAnalyzer analyzer = new RecordingAnalyzer();
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(analyzer);

        adapter.analyze(request());

        assertEquals("session-1", analyzer.sessionId);
        assertEquals("hello", analyzer.message);
        assertEquals("USER: \"hello\" | ASSISTANT: \"Which date would you like?\"", analyzer.recentTurns);
        assertEquals("RESERVATION_CREATE", analyzer.activeWorkflowType);
        assertEquals("reservation_name:TEXT=\"Martin\"", analyzer.collectedData);
        assertEquals("date, time", analyzer.missingRequiredSlots);
        assertEquals("en", analyzer.language);
    }

    @Test
    void deserializesRawEnumValuesThroughFallbackParsers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "language": "fr",
                  "intents": [
                    {
                      "name": "reservation_create",
                      "entities": [
                        {"type": "people_count", "raw_value": "10"}
                      ]
                    }
                  ]
                }
                """;

        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = objectMapper.readValue(
                json,
                ConversationAnalyzerLlm.ConversationAnalysisPayload.class);

        assertEquals(AnalyzedIntentName.RESERVATION_CREATE, payload.intents().getFirst().name());
        assertEquals(AnalyzedEntityType.PEOPLE_COUNT, payload.intents().getFirst().entities().getFirst().type());
    }

    @Test
    void unknownRawEnumValuesFallbackToUnknown() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "language": "fr",
                  "intents": [
                    {
                      "name": "reservation_make_magic",
                      "entities": [
                        {"type": "guest_total_magic", "raw_value": "10"}
                      ]
                    }
                  ]
                }
                """;

        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = assertDoesNotThrow(() -> objectMapper.readValue(
                json,
                ConversationAnalyzerLlm.ConversationAnalysisPayload.class));

        assertEquals(AnalyzedIntentName.UNKNOWN, payload.intents().getFirst().name());
        assertEquals(AnalyzedEntityType.UNKNOWN, payload.intents().getFirst().entities().getFirst().type());
    }

    @Test
    void ignoresEntityWithoutRawValue() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        List.of(new ConversationAnalyzerLlm.IntentPayload(
                                AnalyzedIntentName.RESERVATION_CREATE,
                                List.of(new ConversationAnalyzerLlm.EntityPayload(
                                        AnalyzedEntityType.RESERVATION_NAME,
                                        null)))))));

        var analysis = adapter.analyze(request());

        assertTrue(analysis.firstIntent().orElseThrow().entities().isEmpty());
    }

    @Test
    void mapsUnknownLanguageToNullForApplicationFallback() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        null,
                        List.of())));

        var analysis = adapter.analyze(request());

        assertNull(analysis.language());
    }

    @Test
    void analysisPayloadFieldsExposeDescriptionsForJsonSchema() throws Exception {
        assertTrue(ConversationAnalyzerLlm.ConversationAnalysisPayload.class.isAnnotationPresent(Description.class));
        assertTrue(ConversationAnalyzerLlm.IntentPayload.class
                .getDeclaredField("name")
                .isAnnotationPresent(Description.class));
        assertTrue(ConversationAnalyzerLlm.EntityPayload.class
                .getDeclaredField("type")
                .isAnnotationPresent(Description.class));
    }

    private static ConversationAnalysisRequest request() {
        return new ConversationAnalysisRequest(
                "hello",
                SessionId.of("session-1"),
                EstablishmentId.of("est-1"),
                WorkflowType.RESERVATION_CREATE,
                List.of(new ConversationAnalysisRequest.CollectedValueSnapshot("reservation_name", "TEXT", "Martin")),
                List.of("date", "time"),
                false,
                List.of(
                        new ConversationAnalysisRequest.ConversationTurnSnapshot(ConversationTurn.Role.USER, "hello"),
                        new ConversationAnalysisRequest.ConversationTurnSnapshot(ConversationTurn.Role.ASSISTANT, "Which date would you like?")),
                "en",
                "Europe/Paris",
                LocalDate.of(2026, 5, 23));
    }

    private static final class RecordingAnalyzer implements ConversationAnalyzerLlm {
        private String sessionId = "";
        private String message = "";
        private String recentTurns = "";
        private String activeWorkflowType = "";
        private String collectedData = "";
        private String missingRequiredSlots = "";
        private String language = "";

        @Override
        public ConversationAnalysisPayload analyze(
                String sessionId,
                String message,
                String recentTurns,
                String activeWorkflowType,
                String collectedData,
                String missingRequiredSlots,
                boolean awaitingConfirmation,
                String language) {
            this.sessionId = sessionId;
            this.message = message;
            this.recentTurns = recentTurns;
            this.activeWorkflowType = activeWorkflowType;
            this.collectedData = collectedData;
            this.missingRequiredSlots = missingRequiredSlots;
            this.language = language;
            return new ConversationAnalysisPayload("en", List.of());
        }
    }

    private static final class FixedAnalyzer implements ConversationAnalyzerLlm {
        private final ConversationAnalysisPayload payload;

        private FixedAnalyzer(ConversationAnalysisPayload payload) {
            this.payload = payload;
        }

        @Override
        public ConversationAnalysisPayload analyze(
                String sessionId,
                String message,
                String recentTurns,
                String activeWorkflowType,
                String collectedData,
                String missingRequiredSlots,
                boolean awaitingConfirmation,
                String language) {
            return payload;
        }
    }
}
