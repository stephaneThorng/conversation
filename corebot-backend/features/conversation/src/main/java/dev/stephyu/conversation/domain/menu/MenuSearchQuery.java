package dev.stephyu.conversation.domain.menu;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record MenuSearchQuery(UUID establishmentId) {

    public MenuSearchQuery {
        if (establishmentId == null) {
            throw new IllegalArgumentException("establishmentId must not be null");
        }
    }
}
