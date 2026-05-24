package dev.stephyu.conversation.adapter.outbound.normalizer;

import com.microsoft.recognizers.text.Culture;
import com.microsoft.recognizers.text.ModelResult;
import com.microsoft.recognizers.text.datetime.DateTimeOptions;
import com.microsoft.recognizers.text.datetime.DateTimeRecognizer;
import com.microsoft.recognizers.text.number.NumberRecognizer;
import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.orchestration.SlotNormalizationContext;
import dev.stephyu.conversation.application.orchestration.SlotValueNormalizer;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class RecognizersTextSlotValueNormalizer implements SlotValueNormalizer {

    private static final List<String> SUPPORTED_CULTURES = List.of(
            Culture.English,
            Culture.French,
            Culture.German,
            Culture.Italian,
            Culture.Spanish,
            Culture.Portuguese,
            Culture.Dutch);
    private static final LocalTime REFERENCE_TIME = LocalTime.NOON;

    @Override
    public Optional<SlotDataValue> normalize(AnalyzedEntity entity, SlotName slotName, SlotNormalizationContext context) {
        return switch (slotName.value()) {
            case "reservation_name",
                 "menu_name",
                 "menu_item_name",
                 "menu_ingredient",
                 "menu_allergen_code",
                 "menu_dietary_restriction_code",
                 "menu_price_comparator" -> Optional.of(new SlotDataValue.TextValue(entity.rawValue().trim()));
            case "reference_number" -> Optional.of(new SlotDataValue.TextValue(entity.rawValue().trim().toUpperCase(Locale.ROOT)));
            case "people_count", "menu_price_min_cents", "menu_price_max_cents" -> normalizeNumber(entity, context).map(SlotDataValue.NumberValue::new);
            case "date" -> normalizeDate(entity, context).map(SlotDataValue.DateValue::new);
            case "time" -> normalizeTime(entity, context).map(SlotDataValue.TimeValue::new);
            default -> Optional.empty();
        };
    }

    private Optional<Integer> normalizeNumber(AnalyzedEntity entity, SlotNormalizationContext context) {
        for (String culture : candidateCultures(context.language())) {
            List<ModelResult> results = NumberRecognizer.recognizeNumber(entity.rawValue(), culture);
            for (ModelResult result : results) {
                Optional<Integer> parsed = parseInteger(result.resolution);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> normalizeDate(AnalyzedEntity entity, SlotNormalizationContext context) {
        for (String culture : candidateCultures(context.language())) {
            List<ModelResult> results = DateTimeRecognizer.recognizeDateTime(
                    entity.rawValue(),
                    culture,
                    DateTimeOptions.None,
                    true,
                    referenceDateTime(context));
            for (ModelResult result : results) {
                Optional<LocalDate> parsed = parseDate(result.resolution);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<LocalTime> normalizeTime(AnalyzedEntity entity, SlotNormalizationContext context) {
        for (String culture : candidateCultures(context.language())) {
            List<ModelResult> results = DateTimeRecognizer.recognizeDateTime(
                    entity.rawValue(),
                    culture,
                    DateTimeOptions.None,
                    true,
                    referenceDateTime(context));
            for (ModelResult result : results) {
                Optional<LocalTime> parsed = parseTime(result.resolution);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return Optional.empty();
    }

    private static LocalDateTime referenceDateTime(SlotNormalizationContext context) {
        return context.referenceDate().atTime(REFERENCE_TIME);
    }

    private static List<String> candidateCultures(String language) {
        String preferredCulture = switch (language.toLowerCase(Locale.ROOT)) {
            case "fr" -> Culture.French;
            case "de" -> Culture.German;
            case "it" -> Culture.Italian;
            case "es" -> Culture.Spanish;
            case "pt" -> Culture.Portuguese;
            case "nl" -> Culture.Dutch;
            default -> Culture.English;
        };
        if (preferredCulture.equals(Culture.English)) {
            return SUPPORTED_CULTURES;
        }
        return List.of(
                preferredCulture,
                Culture.English,
                Culture.French,
                Culture.German,
                Culture.Italian,
                Culture.Spanish,
                Culture.Portuguese,
                Culture.Dutch);
    }

    private static Optional<Integer> parseInteger(SortedMap<String, Object> resolution) {
        Object value = resolution.get("value");
        if (!(value instanceof String text)) {
            return Optional.empty();
        }
        try {
            return Optional.of((int) Math.round(Double.parseDouble(text)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<LocalDate> parseDate(SortedMap<String, Object> resolution) {
        Object values = resolution.get("values");
        if (!(values instanceof List<?> entries)) {
            return Optional.empty();
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> rawEntry)) {
                continue;
            }
            Object type = rawEntry.get("type");
            Object value = rawEntry.get("value");
            if (!"date".equals(type) || !(value instanceof String text)) {
                continue;
            }
            try {
                return Optional.of(LocalDate.parse(text));
            } catch (DateTimeParseException ignored) {
                // Continue with another resolution candidate.
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Optional<LocalTime> parseTime(SortedMap<String, Object> resolution) {
        Object values = resolution.get("values");
        if (!(values instanceof List<?> entries)) {
            return Optional.empty();
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> rawEntry)) {
                continue;
            }
            Object type = rawEntry.get("type");
            Object value = rawEntry.get("value");
            if (!"time".equals(type) || !(value instanceof String text)) {
                continue;
            }
            try {
                return Optional.of(LocalTime.parse(text));
            } catch (DateTimeParseException ignored) {
                // Continue with another resolution candidate.
            }
        }
        return Optional.empty();
    }
}
