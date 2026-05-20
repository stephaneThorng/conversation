package dev.stephyu.conversation.application.usecase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public interface HandleConversationUseCase {
    HandleConversationResult handle(HandleConversationCommand command);

    @NullMarked
    record HandleConversationCommand(@Nullable String sessionId, String message) {}
    @NullMarked
    record HandleConversationResult(String sessionId, String reply) {}

}
