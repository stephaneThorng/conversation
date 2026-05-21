package dev.stephyu.conversation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.application.port.outbound.LlmAssistantPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase.HandleConversationCommand;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import org.junit.jupiter.api.Test;

class HandleConversationServiceTest {

    @Test
    void persistsConversationStateWithLastAssistantReply() {
        InMemoryConversationStateRepository repository = new InMemoryConversationStateRepository();
        LlmAssistantPort llm = message -> "reply to " + message;
        HandleConversationService service = new HandleConversationService(
                llm,
                repository);

        var result = service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-1"),
                "hello"));

        assertEquals("session-1", result.sessionId().value());
        assertEquals("reply to hello", result.reply());
        var savedSession = repository.findBySessionId(SessionId.of("session-1")).orElseThrow();
        assertEquals("establishment-1", savedSession.state().establishmentId().value());
        assertEquals("reply to hello", savedSession.state().lastAssistantReply());
    }

    @Test
    void reusesExistingConversationState() {
        InMemoryConversationStateRepository repository = new InMemoryConversationStateRepository();
        HandleConversationService service = new HandleConversationService(
                message -> "new reply",
                repository);

        service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-1"),
                "first"));
        service.handle(new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-2"),
                "second"));

        var savedSession = repository.findBySessionId(SessionId.of("session-1")).orElseThrow();
        assertFalse(savedSession.state().hasActiveWorkflow());
        assertEquals("establishment-1", savedSession.state().establishmentId().value());
        assertEquals("new reply", savedSession.state().lastAssistantReply());
    }
}
