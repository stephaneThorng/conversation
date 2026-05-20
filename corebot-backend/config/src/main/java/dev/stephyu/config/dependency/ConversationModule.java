package dev.stephyu.config.dependency;

import dev.stephyu.conversation.adapter.inbound.web.UserController;
import dev.stephyu.conversation.application.HandleConversationService;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.config.web.HttpEndpoint;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConversationModule {

    public List<HttpEndpoint> httpEndpoints() {
        HandleConversationUseCase handleConversationUseCase = new HandleConversationService();
        UserController userController = new UserController(handleConversationUseCase);
        return List.of(userController::register);
    }
}
