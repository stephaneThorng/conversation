package dev.stephyu.conversation.domain;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EstablishmentId(String value) {

    public EstablishmentId {
        if (value.isBlank()) {
            throw new IllegalArgumentException("establishmentId must not be blank");
        }
    }

    public static EstablishmentId of(String value) {
        return new EstablishmentId(value);
    }
}
