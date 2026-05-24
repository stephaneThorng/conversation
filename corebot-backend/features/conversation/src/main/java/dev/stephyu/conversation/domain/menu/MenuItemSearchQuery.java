package dev.stephyu.conversation.domain.menu;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record MenuItemSearchQuery(UUID establishmentId) {

    public MenuItemSearchQuery {
        if (establishmentId == null) {
            throw new IllegalArgumentException("establishmentId must not be null");
        }
    }
}
