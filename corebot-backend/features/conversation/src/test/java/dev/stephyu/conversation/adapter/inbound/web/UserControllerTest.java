package dev.stephyu.conversation.adapter.inbound.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import org.junit.jupiter.api.Test;

class UserControllerTest {

    @Test
    void canBeInstantiatedWithFakeUseCase() {
        HandleConversationUseCase fakeUseCase =
            command -> new HandleConversationUseCase.HandleConversationResult("session-1", "reply");

        UserController controller = new UserController(fakeUseCase);

        assertNotNull(controller);
    }
}
