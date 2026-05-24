package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.adapter.outbound.reply.StaticEstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationOrchestratorTest {

    // Stub: returns "INTENT" so tests can assert on the intent name without LLM or catalog.
    private static final ConversationReplyPort STUB_REPLY = (sessionId, language, ctx, userMessage) -> ctx.replyIntent().name();

    private static final ReservationRepositoryPort FAKE_REPO = new ReservationRepositoryPort() {
        @Override
        public ReservationResult createReservation(CreateReservationRequest request) {
            return ReservationResult.success("TESTREF");
        }
        @Override
        public java.util.Optional<ReservationSummary> findReservation(String ref) {
            return java.util.Optional.empty();
        }
        @Override
        public ReservationResult cancelReservation(String ref) {
            return ReservationResult.failure("not_found");
        }
    };

    @Test
    void clearsWorkflowOnCancel() {
        ConversationOrchestrator orchestrator = orchestrator(
                request -> new ConversationAnalysis("en", List.of(new AnalyzedIntent(AnalyzedIntentName.CANCEL, List.of()))));

        ConversationSession session = new ConversationSession(
                SessionId.of("session-1"),
                new ConversationState(EstablishmentId.of("est-1"), null).withWorkflow(
                        new ReservationCreateHandler(FAKE_REPO).newWorkflow()));

        var result = orchestrator.orchestrate(session, "cancel");

        assertFalse(result.session().state().hasActiveWorkflow());
        assertEquals("WORKFLOW_CANCELLED", result.reply());
    }

    @Test
    void returnsNotUnderstoodForUnknownIntent() {
        ConversationOrchestrator orchestrator = orchestrator(
                request -> new ConversationAnalysis("en", List.of(new AnalyzedIntent(
                        AnalyzedIntentName.UNKNOWN,
                        List.of(new AnalyzedEntity(AnalyzedEntityType.UNKNOWN, "hello"))))));

        var result = orchestrator.orchestrate(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "hello");

        assertEquals("NOT_UNDERSTOOD", result.reply());
    }

    @Test
    void startsReservationWorkflowFromSingleIntent() {
        ConversationOrchestrator orchestrator = orchestrator(
                request -> new ConversationAnalysis("fr", List.of(new AnalyzedIntent(AnalyzedIntentName.RESERVATION_CREATE, List.of()))));

        var result = orchestrator.orchestrate(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "Bonjour, je souhaite reserver");

        assertTrue(result.session().state().hasActiveWorkflow());
        assertEquals("ASK_SLOT", result.reply());
    }

    private static ConversationOrchestrator orchestrator(dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort analyzerPort) {
        return new ConversationOrchestrator(
                analyzerPort,
                new IntentHandlerRegistry(List.of(new ReservationCreateHandler(FAKE_REPO))),
                STUB_REPLY,
                new StaticEstablishmentResponseStyleResolver(),
                new WorkflowProcessor(new RecognizersTextSlotValueNormalizer()),
                new WorkflowReplyResolver(STUB_REPLY));
    }
}
