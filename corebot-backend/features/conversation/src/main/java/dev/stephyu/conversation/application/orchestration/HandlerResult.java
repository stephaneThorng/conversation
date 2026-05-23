package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.domain.ConversationState;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record HandlerResult(ConversationState state, String reply) {

    public HandlerResult {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(reply, "reply must not be null");
    }
}
