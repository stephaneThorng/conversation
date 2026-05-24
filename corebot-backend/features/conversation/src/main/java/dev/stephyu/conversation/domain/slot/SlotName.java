package dev.stephyu.conversation.domain.slot;

import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SlotName(String value, SlotDataType dataType) {

    public static final SlotName RESERVATION_NAME = new SlotName("reservation_name", SlotDataType.TEXT);
    public static final SlotName DATE = new SlotName("date", SlotDataType.DATE);
    public static final SlotName TIME = new SlotName("time", SlotDataType.TIME);
    public static final SlotName PEOPLE_COUNT = new SlotName("people_count", SlotDataType.NUMBER);
    public static final SlotName REFERENCE_NUMBER = new SlotName("reference_number", SlotDataType.TEXT);
    public static final SlotName MENU_ITEM_NAME = new SlotName("menu_item_name", SlotDataType.TEXT);
    public static final SlotName MENU_NAME = new SlotName("menu_name", SlotDataType.TEXT);
    public static final SlotName MENU_INGREDIENT = new SlotName("menu_ingredient", SlotDataType.TEXT);
    public static final SlotName MENU_ALLERGEN_CODE = new SlotName("menu_allergen_code", SlotDataType.TEXT);
    public static final SlotName MENU_DIETARY_RESTRICTION_CODE = new SlotName("menu_dietary_restriction_code", SlotDataType.TEXT);
    public static final SlotName MENU_PRICE_COMPARATOR = new SlotName("menu_price_comparator", SlotDataType.TEXT);
    public static final SlotName MENU_PRICE_MIN_CENTS = new SlotName("menu_price_min_cents", SlotDataType.NUMBER);
    public static final SlotName MENU_PRICE_MAX_CENTS = new SlotName("menu_price_max_cents", SlotDataType.NUMBER);

    public SlotName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("slot name must not be blank");
        }
    }

    public static SlotName of(String value, SlotDataType dataType) {
        return new SlotName(value, dataType);
    }

    public static Optional<SlotName> fromEntityType(AnalyzedEntityType entityType) {
        return switch (entityType) {
            case RESERVATION_NAME -> Optional.of(RESERVATION_NAME);
            case DATE -> Optional.of(DATE);
            case TIME -> Optional.of(TIME);
            case PEOPLE_COUNT -> Optional.of(PEOPLE_COUNT);
            case REFERENCE_NUMBER -> Optional.of(REFERENCE_NUMBER);
            case MENU_NAME -> Optional.of(MENU_NAME);
            case MENU_ITEM_NAME -> Optional.of(MENU_ITEM_NAME);
            case MENU_INGREDIENT -> Optional.of(MENU_INGREDIENT);
            case MENU_ALLERGEN_CODE -> Optional.of(MENU_ALLERGEN_CODE);
            case MENU_DIETARY_RESTRICTION_CODE -> Optional.of(MENU_DIETARY_RESTRICTION_CODE);
            case MENU_PRICE_COMPARATOR -> Optional.of(MENU_PRICE_COMPARATOR);
            case MENU_PRICE_MIN_CENTS -> Optional.of(MENU_PRICE_MIN_CENTS);
            case MENU_PRICE_MAX_CENTS -> Optional.of(MENU_PRICE_MAX_CENTS);
            case UNKNOWN -> Optional.empty();
        };
    }
}
