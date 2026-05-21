package dev.stephyu.conversation.domain.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CollectedDataTest {

    @Test
    void storesAndReadsCollectedValues() {
        CollectedData data = CollectedData.empty()
                .withValue(SlotName.RESERVATION_NAME, new SlotDataValue.TextValue("Stephane"));

        assertTrue(data.contains(SlotName.RESERVATION_NAME));
        assertEquals(
                "Stephane",
                data.valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class)
                        .orElseThrow()
                        .value());
    }

    @Test
    void replacesCollectedValues() {
        CollectedData data = CollectedData.empty()
                .withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.NumberValue(2))
                .withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.NumberValue(4));

        assertEquals(
                4,
                data.valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.NumberValue.class)
                        .orElseThrow()
                        .value());
    }

    @Test
    void returnsEmptyWhenValueIsAbsentOrHasAnotherType() {
        CollectedData data = new CollectedData(Map.of(
                SlotName.PEOPLE_COUNT,
                new SlotDataValue.NumberValue(2)));

        assertFalse(data.value(SlotName.DATE).isPresent());
        assertFalse(data.valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.TextValue.class).isPresent());
    }

    @Test
    void rejectsValueWithWrongSlotType() {
        CollectedData data = CollectedData.empty();

        assertThrows(
                IllegalArgumentException.class,
                () -> data.withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.TextValue("two")));
    }
}
