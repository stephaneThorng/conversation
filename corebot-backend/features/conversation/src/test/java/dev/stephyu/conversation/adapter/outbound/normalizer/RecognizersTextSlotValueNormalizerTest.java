package dev.stephyu.conversation.adapter.outbound.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.orchestration.SlotNormalizationContext;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RecognizersTextSlotValueNormalizerTest {

    private final RecognizersTextSlotValueNormalizer normalizer = new RecognizersTextSlotValueNormalizer();

    @Test
    void normalizesRelativeNamedDate() {
        var value = normalizer.normalize(
                new AnalyzedEntity(AnalyzedEntityType.DATE, "demain"),
                SlotName.DATE,
                new SlotNormalizationContext("fr", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.DateValue(LocalDate.of(2026, 5, 24)), value);
    }

    @Test
    void normalizesLocalizedAbsoluteDate() {
        var value = normalizer.normalize(
                new AnalyzedEntity(AnalyzedEntityType.DATE, "23 mayo 2026"),
                SlotName.DATE,
                new SlotNormalizationContext("es", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.DateValue(LocalDate.of(2026, 5, 23)), value);
    }

    @Test
    void prefersRelativeWeekdayOverWrongLlmNormalizedValue() {
        var value = normalizer.normalize(
                        new AnalyzedEntity(AnalyzedEntityType.DATE, "mardi prochain"),
                        SlotName.DATE,
                        new SlotNormalizationContext("fr", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.DateValue(LocalDate.of(2026, 5, 26)), value);
    }

    @Test
    void normalizesFrenchRelativeWeekdayEvenWhenConversationLanguageIsEnglish() {
        var value = normalizer.normalize(
                        new AnalyzedEntity(AnalyzedEntityType.DATE, "lundi prochain"),
                        SlotName.DATE,
                        new SlotNormalizationContext("en", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.DateValue(LocalDate.of(2026, 5, 25)), value);
    }

    @Test
    void normalizesNumberFromWord() {
        var value = normalizer.normalize(
                new AnalyzedEntity(AnalyzedEntityType.PEOPLE_COUNT, "four"),
                SlotName.PEOPLE_COUNT,
                new SlotNormalizationContext("en", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.NumberValue(4), value);
    }

    @Test
    void normalizesTimeFromRawValue() {
        var value = normalizer.normalize(
                new AnalyzedEntity(AnalyzedEntityType.TIME, "6pm"),
                SlotName.TIME,
                new SlotNormalizationContext("en", ZoneId.of("Europe/Paris"), LocalDate.of(2026, 5, 23)))
                .orElseThrow();

        assertEquals(new SlotDataValue.TimeValue(java.time.LocalTime.of(18, 0)), value);
    }
}
