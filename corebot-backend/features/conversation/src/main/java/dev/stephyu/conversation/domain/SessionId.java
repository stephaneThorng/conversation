package dev.stephyu.conversation.domain;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record SessionId(String value) {

    public SessionId {
        if (value.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    public static SessionId of(String value) {
        return new SessionId(value);
    }

    public static SessionId generate() {
        return of(UUID.randomUUID().toString());
    }
}
