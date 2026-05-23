package dev.stephyu.conversation.application.usecase;

import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface HandleConversationUseCase {
    HandleConversationResult handle(HandleConversationCommand command);

    @NullMarked
    record HandleConversationCommand(SessionId sessionId, EstablishmentId establishmentId, String message) {
        public HandleConversationCommand {
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }

    @NullMarked
    record HandleConversationResult(SessionId sessionId, String reply) {}

}
