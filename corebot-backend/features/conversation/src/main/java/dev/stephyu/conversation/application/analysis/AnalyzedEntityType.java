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
    MENU_NAME,
    MENU_ITEM_NAME,
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
            case "menuname" -> MENU_NAME;
            case "menuitemname" -> MENU_ITEM_NAME;
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
