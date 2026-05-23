package dev.stephyu.conversation.domain;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConversationTurn(Role role, String content) {

    public ConversationTurn {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    public enum Role {
        USER,
        ASSISTANT
    }
}
