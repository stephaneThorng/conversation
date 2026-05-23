package dev.stephyu.conversation.domain.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import org.junit.jupiter.api.Test;

class SlotNameTest {

    @Test
    void mapsKnownEntityTypeToSlotName() {
        assertEquals(SlotName.RESERVATION_NAME, SlotName.fromEntityType(AnalyzedEntityType.RESERVATION_NAME).orElseThrow());
    }

    @Test
    void returnsEmptyForUnknownEntityType() {
        assertTrue(SlotName.fromEntityType(AnalyzedEntityType.UNKNOWN).isEmpty());
    }
}
