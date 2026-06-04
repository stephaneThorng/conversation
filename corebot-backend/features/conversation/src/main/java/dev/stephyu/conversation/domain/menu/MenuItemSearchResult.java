package dev.stephyu.conversation.domain.menu;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record MenuItemSearchResult(
        UUID menuItemId,
        String menuItemCode,
        Map<String, String> nameTranslations,
        Map<String, String> descriptionTranslations,
        Map<String, String> ingredientNoteTranslations,
        int priceCents,
        String currency,
        List<String> categoryCodes,
        List<String> allergenCodes,
        List<String> dietaryRestrictionCodes
) {
    public MenuItemSearchResult {
        nameTranslations = Map.copyOf(nameTranslations);
        descriptionTranslations = Map.copyOf(descriptionTranslations);
        ingredientNoteTranslations = Map.copyOf(ingredientNoteTranslations);
        categoryCodes = List.copyOf(categoryCodes);
        allergenCodes = List.copyOf(allergenCodes);
        dietaryRestrictionCodes = List.copyOf(dietaryRestrictionCodes);
    }
}
