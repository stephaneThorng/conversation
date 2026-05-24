package dev.stephyu.conversation.domain.menu;

import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record MenuItemSearchQuery(
        UUID establishmentId,
        @Nullable String itemName,
        @Nullable String ingredient,
        @Nullable String allergenCode,
        @Nullable String dietaryRestrictionCode,
        @Nullable PriceCriterion priceCriterion
) {

    public MenuItemSearchQuery {
        if (establishmentId == null) {
            throw new IllegalArgumentException("establishmentId must not be null");
        }
    }

    public Optional<String> itemNameFilter() {
        return normalize(itemName);
    }

    public Optional<String> ingredientFilter() {
        return normalize(ingredient);
    }

    public Optional<String> allergenCodeFilter() {
        return normalize(allergenCode);
    }

    public Optional<String> dietaryRestrictionCodeFilter() {
        return normalize(dietaryRestrictionCode);
    }

    private static Optional<String> normalize(@Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }
}
