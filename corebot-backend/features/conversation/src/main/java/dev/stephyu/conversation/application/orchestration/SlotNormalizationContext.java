package dev.stephyu.conversation.application.orchestration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SlotNormalizationContext(
        String language,
        ZoneId zoneId,
        LocalDate referenceDate
) {

    public SlotNormalizationContext {
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");
        Objects.requireNonNull(referenceDate, "referenceDate must not be null");
    }

    public Locale locale() {
        return Locale.forLanguageTag(language);
    }
}
