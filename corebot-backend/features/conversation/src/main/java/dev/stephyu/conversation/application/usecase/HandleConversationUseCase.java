package dev.stephyu.conversation.application.usecase;

import dev.stephyu.conversation.domain.Channel;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface HandleConversationUseCase {
    HandleConversationResult handle(HandleConversationCommand command);

    @NullMarked
    record HandleConversationCommand(
            SessionId sessionId,
            EstablishmentId establishmentId,
            String channelUserId,
            Channel channel,
            String message) {
        public HandleConversationCommand {
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
            if (channelUserId.isBlank()) {
                throw new IllegalArgumentException("channelUserId must not be blank");
            }
        }
    }

    @NullMarked
    record HandleConversationResult(SessionId sessionId, String reply) {}

}
