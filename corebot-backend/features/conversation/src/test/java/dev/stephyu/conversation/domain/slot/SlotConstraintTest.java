package dev.stephyu.conversation.domain.slot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SlotConstraintTest {

    @Test
    void validatesTextMaxLength() {
        SlotConstraint constraint = new SlotConstraint.TextMaxLen(3);

        assertFalse(constraint.violationKey(new SlotDataValue.TextValue("abc")).isPresent());
        assertTrue(constraint.violationKey(new SlotDataValue.TextValue("abcd")).isPresent());
    }

    @Test
    void validatesEmailFormat() {
        SlotConstraint constraint = new SlotConstraint.EmailFormat();

        assertFalse(constraint.violationKey(new SlotDataValue.TextValue("user@example.com")).isPresent());
        assertTrue(constraint.violationKey(new SlotDataValue.TextValue("invalid")).isPresent());
    }

    @Test
    void validatesNumberRange() {
        SlotConstraint constraint = new SlotConstraint.NumberRange(1, 6);

        assertFalse(constraint.violationKey(new SlotDataValue.NumberValue(4)).isPresent());
        assertTrue(constraint.violationKey(new SlotDataValue.NumberValue(7)).isPresent());
    }

    @Test
    void validatesFutureDate() {
        SlotConstraint constraint = new SlotConstraint.FutureDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        assertFalse(constraint.violationKey(new SlotDataValue.DateValue(today)).isPresent());
        assertTrue(constraint.violationKey(new SlotDataValue.DateValue(today.minusDays(1))).isPresent());
    }
}
