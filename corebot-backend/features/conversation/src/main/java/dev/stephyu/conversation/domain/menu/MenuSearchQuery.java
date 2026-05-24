package dev.stephyu.conversation.domain.menu;

import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record MenuSearchQuery(
        UUID establishmentId,
        @Nullable String menuName,
        @Nullable String ingredient,
        @Nullable PriceCriterion priceCriterion
) {

    public MenuSearchQuery {
        if (establishmentId == null) {
            throw new IllegalArgumentException("establishmentId must not be null");
        }
    }

    public Optional<String> menuNameFilter() {
        return normalize(menuName);
    }

    public Optional<String> ingredientFilter() {
        return normalize(ingredient);
    }

    private static Optional<String> normalize(@Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }
}
