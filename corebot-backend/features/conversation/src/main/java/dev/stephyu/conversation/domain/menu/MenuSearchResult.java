package dev.stephyu.conversation.domain.menu;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record MenuSearchResult(
        UUID menuId,
        String menuCode,
        Map<String, String> nameTranslations,
        Map<String, String> descriptionTranslations,
        @Nullable Integer priceCents,
        String currency,
        List<MenuSectionResult> sections
) {
    public MenuSearchResult {
        nameTranslations = Map.copyOf(nameTranslations);
        descriptionTranslations = Map.copyOf(descriptionTranslations);
        sections = List.copyOf(sections);
    }

    @NullMarked
    public record MenuSectionResult(
            UUID sectionId,
            String sectionCode,
            Map<String, String> nameTranslations,
            Map<String, String> descriptionTranslations,
            List<MenuCompositionItemResult> items
    ) {
        public MenuSectionResult {
            nameTranslations = Map.copyOf(nameTranslations);
            descriptionTranslations = Map.copyOf(descriptionTranslations);
            items = List.copyOf(items);
        }
    }

    @NullMarked
    public record MenuCompositionItemResult(
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
        public MenuCompositionItemResult {
            nameTranslations = Map.copyOf(nameTranslations);
            descriptionTranslations = Map.copyOf(descriptionTranslations);
            ingredientNoteTranslations = Map.copyOf(ingredientNoteTranslations);
            categoryCodes = List.copyOf(categoryCodes);
            allergenCodes = List.copyOf(allergenCodes);
            dietaryRestrictionCodes = List.copyOf(dietaryRestrictionCodes);
        }
    }
}
