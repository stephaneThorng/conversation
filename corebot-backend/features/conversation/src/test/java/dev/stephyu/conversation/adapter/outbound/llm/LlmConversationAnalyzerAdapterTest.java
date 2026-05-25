package dev.stephyu.conversation.adapter.outbound.llm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class LlmConversationAnalyzerAdapterTest {

    @Test
    void mapsKnownValuesToEnums() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "en",
                        AnalyzedIntentName.RESERVATION_CREATE,
                        false, false, false,
                         new ConversationAnalyzerLlm.ReservationDetailsPayload("Martin", null, null, null, null))));

        var analysis = adapter.analyze(request("Martin"));

        assertEquals(AnalyzedIntentName.RESERVATION_CREATE, analysis.firstIntent().orElseThrow().name());
        assertEquals(AnalyzedEntityType.RESERVATION_NAME, analysis.firstIntent().orElseThrow().entities().getFirst().type());
        assertEquals("Martin", analysis.firstIntent().orElseThrow().entities().getFirst().rawValue());
    }

    @Test
    void mapsAllReservationDetailsToEntities() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CREATE,
                        false, false, false,
                         new ConversationAnalyzerLlm.ReservationDetailsPayload("Stephane", "5", "lundi prochain", "20h30", null))));
        var analysis = adapter.analyze(request("Stephane 5 lundi prochain 20h30"));
        var entities = analysis.firstIntent().orElseThrow().entities();

        assertEquals(4, entities.size());
        assertEquals(AnalyzedEntityType.RESERVATION_NAME, entities.get(0).type());
        assertEquals("Stephane", entities.get(0).rawValue());
        assertEquals(AnalyzedEntityType.PEOPLE_COUNT, entities.get(1).type());
        assertEquals("5", entities.get(1).rawValue());
        assertEquals(AnalyzedEntityType.DATE, entities.get(2).type());
        assertEquals("lundi prochain", entities.get(2).rawValue());
        assertEquals(AnalyzedEntityType.TIME, entities.get(3).type());
        assertEquals("20h30", entities.get(3).rawValue());
    }

    @Test
    void noEntitiesWhenReservationDetailsIsNull() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CREATE,
                        false, false, false,
                        null)));

        var analysis = adapter.analyze(request());

        assertTrue(analysis.firstIntent().orElseThrow().entities().isEmpty());
    }

    @Test
    void controlBooleanFlagsProduceSecondaryIntents() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.UNKNOWN,
                        true, false, false,
                        null)));

        var analysis = adapter.analyze(request());

        assertEquals(1, analysis.intents().size());
        assertEquals(AnalyzedIntentName.UNKNOWN, analysis.intents().getFirst().name());
        assertTrue(analysis.affirmative());
        assertFalse(analysis.negative());
    }

    @Test
    void negativeControlFlagProducesNegativeSignal() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.UNKNOWN,
                        false, true, false,
                        null)));

        var analysis = adapter.analyze(request());

        assertEquals(1, analysis.intents().size());
        assertFalse(analysis.affirmative());
        assertTrue(analysis.negative());
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
        assertTrue(analyzer.analysisHints.contains("Active workflow: RESERVATION_CREATE."));
        assertTrue(analyzer.analysisHints.contains("Do not reuse collectedData or history as extracted values."));
        assertEquals("en", analyzer.language);
    }

    @Test
    void noWorkflowHintsKeepBareCancelAmbiguous() {
        RecordingAnalyzer analyzer = new RecordingAnalyzer();
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(analyzer);

        adapter.analyze(request("annuler", null, List.of(), false));

        assertEquals("none", analyzer.activeWorkflowType);
        assertTrue(analyzer.analysisHints.contains("Use RESERVATION_CANCEL only if the user explicitly refers to an existing reservation."));
        assertTrue(analyzer.analysisHints.contains("A bare cancel, annuler, or stop is ambiguous and should prefer UNKNOWN."));
    }

    @Test
    void activeWorkflowHintsMapBareCancelToWorkflowAbort() {
        RecordingAnalyzer analyzer = new RecordingAnalyzer();
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(analyzer);

        adapter.analyze(request("annuler", WorkflowType.RESERVATION_CREATE, List.of("reservation_name"), false));

        assertEquals("RESERVATION_CREATE", analyzer.activeWorkflowType);
        assertTrue(analyzer.analysisHints.contains("A bare cancel, annuler, or stop means CANCEL for the current workflow."));
    }

    @Test
    void menuIntentDoesNotProduceMenuEntities() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.ASK_MENU_ITEM,
                        false, false, false,
                        null)));

        var analysis = adapter.analyze(request("quels plats vegan a moins de 10 euros ?"));

        assertEquals(AnalyzedIntentName.ASK_MENU_ITEM, analysis.firstIntent().orElseThrow().name());
        assertTrue(analysis.firstIntent().orElseThrow().entities().isEmpty());
    }

    @Test
    void rejectsReservationDetailsAbsentFromLatestMessage() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CREATE,
                        false, false, false,
                        new ConversationAnalyzerLlm.ReservationDetailsPayload(null, null, "2026-05-25", null, null))));

        var analysis = adapter.analyze(request("le 26 mai"));

        assertTrue(analysis.firstIntent().orElseThrow().entities().isEmpty());
    }

    @Test
    void keepsReservationDetailsPresentInLatestMessage() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CREATE,
                        false, false, false,
                        new ConversationAnalyzerLlm.ReservationDetailsPayload(null, null, "le 26 mai", null, null))));

        var analysis = adapter.analyze(request("je prefere le 26 mai"));
        var entities = analysis.firstIntent().orElseThrow().entities();

        assertEquals(1, entities.size());
        assertEquals(AnalyzedEntityType.DATE, entities.getFirst().type());
        assertEquals("le 26 mai", entities.getFirst().rawValue());
    }

    @Test
    void keepsReferenceNumberWhenPrefixWasStripped() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CHECK,
                        false, false, false,
                        new ConversationAnalyzerLlm.ReservationDetailsPayload(null, null, null, null, "1EBC495E"))));

        var analysis = adapter.analyze(request("reference 1EBC495E"));
        var entities = analysis.firstIntent().orElseThrow().entities();

        assertEquals(1, entities.size());
        assertEquals(AnalyzedEntityType.REFERENCE_NUMBER, entities.getFirst().type());
        assertEquals("1EBC495E", entities.getFirst().rawValue());
    }

    @Test
    void keepsReservationCancelIntentWhenNoReferenceYet() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.RESERVATION_CANCEL,
                        false, false, false,
                        null)));

        var analysis = adapter.analyze(request("je souhaite annuler ma reservation", null, List.of(), false));

        assertEquals(AnalyzedIntentName.RESERVATION_CANCEL, analysis.firstIntent().orElseThrow().name());
        assertTrue(analysis.firstIntent().orElseThrow().entities().isEmpty());
    }

    @Test
    void keepsUnknownIntentForBareCancelWhenNoWorkflow() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        "fr",
                        AnalyzedIntentName.UNKNOWN,
                        false, false, false,
                        null)));

        var analysis = adapter.analyze(request("annuler", null, List.of(), false));

        assertEquals(AnalyzedIntentName.UNKNOWN, analysis.firstIntent().orElseThrow().name());
        assertFalse(analysis.affirmative());
        assertFalse(analysis.negative());
    }

    @Test
    void deserializesRawJsonThroughFallbackParsers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "language": "fr",
                  "mainIntent": "RESERVATION_CREATE",
                  "isAffirmative": false,
                  "isNegative": false,
                  "isCancel": false,
                  "reservationDetails": {
                    "customerName": "Richard",
                    "peopleCount": "5",
                    "date": null,
                    "time": null
                  }
                }
                """;

        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = objectMapper.readValue(
                json,
                ConversationAnalyzerLlm.ConversationAnalysisPayload.class);

        assertEquals(AnalyzedIntentName.RESERVATION_CREATE, payload.mainIntent());
        assertEquals("Richard", payload.reservationDetails().customerName());
        assertEquals("5", payload.reservationDetails().peopleCount());
    }

    @Test
    void confirmationDeserializesWithUnknownMainIntentAndAffirmativeFlag() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "language": "fr",
                  "mainIntent": "UNKNOWN",
                  "isAffirmative": true,
                  "isNegative": false,
                  "isCancel": false,
                  "reservationDetails": null
                }
                """;

        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = objectMapper.readValue(
                json,
                ConversationAnalyzerLlm.ConversationAnalysisPayload.class);

        assertEquals(AnalyzedIntentName.UNKNOWN, payload.mainIntent());
        assertTrue(payload.isAffirmative());
    }

    @Test
    void unknownRawIntentValueFallsBackToUnknown() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "language": "fr",
                  "mainIntent": "MAKE_MAGIC",
                  "isAffirmative": false,
                  "isNegative": false,
                  "isCancel": false,
                  "reservationDetails": null
                }
                """;

        ConversationAnalyzerLlm.ConversationAnalysisPayload payload = assertDoesNotThrow(() -> objectMapper.readValue(
                json,
                ConversationAnalyzerLlm.ConversationAnalysisPayload.class));

        assertEquals(AnalyzedIntentName.UNKNOWN, payload.mainIntent());
    }

    @Test
    void mapsUnknownLanguageToNullForApplicationFallback() {
        LlmConversationAnalyzerAdapter adapter = new LlmConversationAnalyzerAdapter(
                new FixedAnalyzer(new ConversationAnalyzerLlm.ConversationAnalysisPayload(
                        null,
                        null,
                        false, false, false,
                        null)));

        var analysis = adapter.analyze(request());

        assertNull(analysis.language());
    }

    @Test
    void analysisPayloadFieldsExposeDescriptionsForJsonSchema() throws Exception {
        assertTrue(ConversationAnalyzerLlm.ConversationAnalysisPayload.class.isAnnotationPresent(Description.class));
        assertTrue(ConversationAnalyzerLlm.ConversationAnalysisPayload.class
                .getDeclaredField("mainIntent")
                .isAnnotationPresent(Description.class));
        assertTrue(ConversationAnalyzerLlm.ReservationDetailsPayload.class
                .getDeclaredField("customerName")
                .isAnnotationPresent(Description.class));
    }

    private static ConversationAnalysisRequest request() {
        return request("hello");
    }

    private static ConversationAnalysisRequest request(String message) {
        return request(message, WorkflowType.RESERVATION_CREATE, List.of("date", "time"), false);
    }

    private static ConversationAnalysisRequest request(
            String message,
            @Nullable WorkflowType workflowType,
            List<String> missingRequiredSlots,
            boolean awaitingConfirmation) {
        return new ConversationAnalysisRequest(
                message,
                SessionId.of("session-1"),
                EstablishmentId.of("est-1"),
                workflowType,
                workflowType == null
                        ? List.of()
                        : List.of(new ConversationAnalysisRequest.CollectedValueSnapshot("reservation_name", "TEXT", "Martin")),
                workflowType == null ? List.of() : missingRequiredSlots,
                workflowType != null && awaitingConfirmation,
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
        private String analysisHints = "";
        private String language = "";

        @Override
        public ConversationAnalysisPayload analyze(
                String sessionId,
                String message,
                String analysisHints,
                String recentTurns,
                String activeWorkflowType,
                String collectedData,
                String missingRequiredSlots,
                boolean awaitingConfirmation,
                String language) {
            this.sessionId = sessionId;
            this.message = message;
            this.analysisHints = analysisHints;
            this.recentTurns = recentTurns;
            this.activeWorkflowType = activeWorkflowType;
            this.collectedData = collectedData;
            this.missingRequiredSlots = missingRequiredSlots;
            this.language = language;
            return new ConversationAnalysisPayload("en", AnalyzedIntentName.UNKNOWN, false, false, false, null);
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
                String analysisHints,
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
