package dev.stephyu.conversation.application.analysis;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum AnalyzedIntentName {
    RESERVATION_CREATE,
    AFFIRMATIVE,
    NEGATIVE,
    CANCEL,
    GREETING,
    THANKS,
    GOODBYE,
    UNKNOWN;

    @JsonCreator
    public static AnalyzedIntentName fromRaw(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return UNKNOWN;
        }
        return switch (normalize(rawValue)) {
            case "reservationcreate" -> RESERVATION_CREATE;
            case "affirmative" -> AFFIRMATIVE;
            case "negative" -> NEGATIVE;
            case "cancel" -> CANCEL;
            case "greeting" -> GREETING;
            case "thanks" -> THANKS;
            case "goodbye" -> GOODBYE;
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
