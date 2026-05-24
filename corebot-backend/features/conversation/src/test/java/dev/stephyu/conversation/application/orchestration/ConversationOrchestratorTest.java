package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.adapter.outbound.reply.PropertiesConversationReplyCatalog;
import dev.stephyu.conversation.adapter.outbound.reply.StaticEstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationOrchestratorTest {

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
        var replyCatalog = new PropertiesConversationReplyCatalog();
        ConversationOrchestrator orchestrator = new ConversationOrchestrator(
                request -> new ConversationAnalysis(
                        "en",
                        List.of(new AnalyzedIntent(AnalyzedIntentName.CANCEL, List.of()))),
                new IntentHandlerRegistry(List.of(new ReservationCreateHandler(FAKE_REPO))),
                replyCatalog,
                new StaticEstablishmentResponseStyleResolver(),
                new WorkflowProcessor(new RecognizersTextSlotValueNormalizer()),
                new WorkflowReplyResolver(replyCatalog));

        ConversationSession session = new ConversationSession(
                SessionId.of("session-1"),
                new ConversationState(EstablishmentId.of("est-1"), null).withWorkflow(
                        new ReservationCreateHandler(FAKE_REPO).newWorkflow()));

        var result = orchestrator.orchestrate(session, "cancel");

        assertFalse(result.session().state().hasActiveWorkflow());
        assertEquals("The current request is cancelled.", result.reply());
    }

    @Test
    void returnsNotUnderstoodForUnknownIntent() {
        var replyCatalog = new PropertiesConversationReplyCatalog();
        ConversationOrchestrator orchestrator = new ConversationOrchestrator(
                request -> new ConversationAnalysis(
                        "en",
                        List.of(new AnalyzedIntent(
                                AnalyzedIntentName.UNKNOWN,
                                List.of(new AnalyzedEntity(AnalyzedEntityType.UNKNOWN, "hello"))))),
                new IntentHandlerRegistry(List.of(new ReservationCreateHandler(FAKE_REPO))),
                replyCatalog,
                new StaticEstablishmentResponseStyleResolver(),
                new WorkflowProcessor(new RecognizersTextSlotValueNormalizer()),
                new WorkflowReplyResolver(replyCatalog));

        var result = orchestrator.orchestrate(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "hello");

        assertEquals("I did not understand that request.", result.reply());
    }

    @Test
    void startsReservationWorkflowFromSingleIntent() {
        var replyCatalog = new PropertiesConversationReplyCatalog();
        ConversationOrchestrator orchestrator = new ConversationOrchestrator(
                request -> new ConversationAnalysis(
                        "fr",
                        List.of(
                                new AnalyzedIntent(AnalyzedIntentName.RESERVATION_CREATE, List.of()))),
                new IntentHandlerRegistry(List.of(new ReservationCreateHandler(FAKE_REPO))),
                replyCatalog,
                new StaticEstablishmentResponseStyleResolver(),
                new WorkflowProcessor(new RecognizersTextSlotValueNormalizer()),
                new WorkflowReplyResolver(replyCatalog));

        var result = orchestrator.orchestrate(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "Bonjour, je souhaite reserver");

        assertTrue(result.session().state().hasActiveWorkflow());
        assertEquals("Quel nom dois-je utiliser pour la reservation ?", result.reply());
    }
}
