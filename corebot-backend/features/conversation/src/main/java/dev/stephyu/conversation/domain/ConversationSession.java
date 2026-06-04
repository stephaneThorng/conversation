package dev.stephyu.conversation.domain;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConversationSession(SessionId sessionId, ConversationState state) {

    public ConversationSession {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
