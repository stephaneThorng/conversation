package dev.stephyu.conversation.application;

import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class HandleConversationService implements HandleConversationUseCase {
    @Override
    public HandleConversationResult handle(HandleConversationCommand command) {
        return new HandleConversationResult("1", "Hello world");
    }
}
