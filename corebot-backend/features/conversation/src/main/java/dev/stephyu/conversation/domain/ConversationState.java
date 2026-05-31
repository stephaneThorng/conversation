package dev.stephyu.conversation.domain;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Minimal session state — the LLM agent (DeepSeek V4 Pro) manages conversation context
 * via ChatMemory. This record only holds establishment context needed for routing.
 */
@NullMarked
public record ConversationState(EstablishmentId establishmentId) {

    public ConversationState {
        Objects.requireNonNull(establishmentId, "establishmentId must not be null");
    }
}
