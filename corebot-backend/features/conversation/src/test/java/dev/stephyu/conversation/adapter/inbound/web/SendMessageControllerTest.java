package dev.stephyu.conversation.adapter.inbound.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase.HandleConversationCommand;
import dev.stephyu.conversation.domain.Channel;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import org.junit.jupiter.api.Test;

class SendMessageControllerTest {

    @Test
    void canBeInstantiatedWithFakeUseCase() {
        HandleConversationUseCase fakeUseCase =
            command -> new HandleConversationUseCase.HandleConversationResult(SessionId.of("session-1"), "reply");

        SendMessageController controller = new SendMessageController(fakeUseCase);

        assertNotNull(controller);
    }

    @Test
    void useCaseCommandRequiresSessionId() {
        HandleConversationCommand command = new HandleConversationCommand(
                SessionId.of("session-1"),
                EstablishmentId.of("establishment-1"),
                "0681234324",
                Channel.WHATSAPP,
                "message");

        assertFalse(command.sessionId().value().isBlank());
    }
}
