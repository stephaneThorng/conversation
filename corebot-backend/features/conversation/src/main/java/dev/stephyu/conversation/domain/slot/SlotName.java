package dev.stephyu.conversation.domain.slot;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SlotName(String value, SlotDataType dataType) {

    public static final SlotName RESERVATION_NAME = new SlotName("reservation_name", SlotDataType.TEXT);
    public static final SlotName DATE = new SlotName("date", SlotDataType.DATE);
    public static final SlotName TIME = new SlotName("time", SlotDataType.TIME);
    public static final SlotName PEOPLE_COUNT = new SlotName("people_count", SlotDataType.NUMBER);
    public static final SlotName MENU_ITEM_NAME = new SlotName("menu_item_name", SlotDataType.TEXT);
    public static final SlotName MENU_NAME = new SlotName("menu_name", SlotDataType.TEXT);

    public SlotName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("slot name must not be blank");
        }
    }

    public static SlotName of(String value, SlotDataType dataType) {
        return new SlotName(value, dataType);
    }
}
