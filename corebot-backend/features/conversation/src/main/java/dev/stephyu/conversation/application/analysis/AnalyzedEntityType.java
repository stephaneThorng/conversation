package dev.stephyu.conversation.application.analysis;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum AnalyzedEntityType {
    RESERVATION_NAME,
    DATE,
    TIME,
    PEOPLE_COUNT,
    REFERENCE_NUMBER,
    MENU_NAME,
    MENU_ITEM_NAME,
    MENU_INGREDIENT,
    MENU_ALLERGEN_CODE,
    MENU_DIETARY_RESTRICTION_CODE,
    MENU_PRICE_COMPARATOR,
    MENU_PRICE_MIN_CENTS,
    MENU_PRICE_MAX_CENTS,
    UNKNOWN;

    @JsonCreator
    public static AnalyzedEntityType fromRaw(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return UNKNOWN;
        }
        return switch (normalize(rawValue)) {
            case "reservationname" -> RESERVATION_NAME;
            case "date" -> DATE;
            case "time" -> TIME;
            case "peoplecount" -> PEOPLE_COUNT;
            case "referencenumber", "reference" -> REFERENCE_NUMBER;
            case "menuname" -> MENU_NAME;
            case "menuitemname" -> MENU_ITEM_NAME;
            case "ingredient", "menuingredient" -> MENU_INGREDIENT;
            case "allergen", "allergencode", "menuallergen" -> MENU_ALLERGEN_CODE;
            case "diet", "dietaryrestriction", "dietaryrestrictioncode", "menudiet" -> MENU_DIETARY_RESTRICTION_CODE;
            case "pricecomparator", "comparator" -> MENU_PRICE_COMPARATOR;
            case "pricemin", "minprice", "minpricecents" -> MENU_PRICE_MIN_CENTS;
            case "pricemax", "maxprice", "maxpricecents" -> MENU_PRICE_MAX_CENTS;
            default -> UNKNOWN;
        };
    }

    private static String normalize(String rawValue) {
        return rawValue
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
