package dev.stephyu.conversation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.adapter.outbound.persistence.FakeReservationAdapter;
import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.adapter.outbound.reply.PropertiesConversationReplyCatalog;
import dev.stephyu.conversation.adapter.outbound.reply.StaticEstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntent;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.orchestration.ConversationOrchestrator;
import dev.stephyu.conversation.application.orchestration.IntentHandlerRegistry;
import dev.stephyu.conversation.application.orchestration.ReservationCreateHandler;
import dev.stephyu.conversation.application.orchestration.WorkflowProcessor;
import dev.stephyu.conversation.application.orchestration.WorkflowReplyResolver;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase.HandleConversationCommand;
import dev.stephyu.conversation.domain.ConversationTurn;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class HandleConversationServiceTest {

    @Test
    void persistsConversationStateWithLastAssistantReply() {
        InMemoryConversationStateRepository repository = new InMemoryConversationStateRepository();
        HandleConversationService service = new HandleConversationService(
                repository,
                createOrchestrator(request -> new ConversationAnalysis(
                        "en",
                        List.of(new AnalyzedIntent(
                                AnalyzedIntentName.RESERVATION_CREATE,
                                List.of(new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin")))))));

        var result = service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-1"),
                "hello"));

        assertEquals("session-1", result.sessionId().value());
        assertEquals("Which date would you like?", result.reply());
        var savedSession = repository.findBySessionId(SessionId.of("session-1")).orElseThrow();
        assertEquals("establishment-1", savedSession.state().establishmentId().value());
        assertEquals("Which date would you like?", savedSession.state().lastAssistantReply().orElseThrow());
        assertEquals("en", savedSession.state().language());
        assertEquals(2, savedSession.state().recentTurns().size());
        assertEquals(ConversationTurn.Role.USER, savedSession.state().recentTurns().getFirst().role());
        assertEquals("hello", savedSession.state().recentTurns().getFirst().content());
        assertEquals(ConversationTurn.Role.ASSISTANT, savedSession.state().recentTurns().getLast().role());
    }

    @Test
    void reusesExistingConversationState() {
        InMemoryConversationStateRepository repository = new InMemoryConversationStateRepository();
        HandleConversationService service = new HandleConversationService(
                repository,
                createOrchestrator(request -> new ConversationAnalysis(
                        "en",
                        List.of(new AnalyzedIntent(
                                AnalyzedIntentName.RESERVATION_CREATE,
                                List.of(new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin")))))));

        service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-1"),
                "first"));
        service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-2"),
                "second"));

        var savedSession = repository.findBySessionId(SessionId.of("session-1")).orElseThrow();
        assertFalse(savedSession.state().activeWorkflow().orElseThrow().collectedData().values().isEmpty());
        assertEquals("establishment-1", savedSession.state().establishmentId().value());
        assertEquals("Which date would you like?", savedSession.state().lastAssistantReply().orElseThrow());
    }

    @Test
    void keepsOnlyTenRecentTurns() {
        InMemoryConversationStateRepository repository = new InMemoryConversationStateRepository();
        HandleConversationService service = new HandleConversationService(
                repository,
                createOrchestrator(request -> new ConversationAnalysis("en", List.of())));

        for (int index = 1; index <= 6; index++) {
            service.handle(new HandleConversationCommand(
                    SessionId.of("session-1"),
                    EstablishmentId.of("establishment-1"),
                    "message-" + index));
        }

        var savedSession = repository.findBySessionId(SessionId.of("session-1")).orElseThrow();
        assertEquals(10, savedSession.state().recentTurns().size());
        assertEquals("message-2", savedSession.state().recentTurns().getFirst().content());
        assertEquals("I did not understand that request.", savedSession.state().recentTurns().getLast().content());
    }

    private static ConversationOrchestrator createOrchestrator(ConversationAnalyzerPort analyzerPort) {
        var replyCatalog = new PropertiesConversationReplyCatalog();
        var reservationHandler = new ReservationCreateHandler(new FakeReservationAdapter());
        return new ConversationOrchestrator(
                analyzerPort,
                new IntentHandlerRegistry(List.of(reservationHandler)),
                replyCatalog,
                new StaticEstablishmentResponseStyleResolver(),
                new WorkflowProcessor(new RecognizersTextSlotValueNormalizer()),
                new WorkflowReplyResolver(replyCatalog));
    }
}
