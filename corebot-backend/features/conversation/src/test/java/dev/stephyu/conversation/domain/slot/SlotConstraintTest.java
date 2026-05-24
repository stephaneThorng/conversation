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

        assertFalse(constraint.isViolated(new SlotDataValue.TextValue("abc")));
        assertTrue(constraint.isViolated(new SlotDataValue.TextValue("abcd")));
    }

    @Test
    void validatesEmailFormat() {
        SlotConstraint constraint = new SlotConstraint.EmailFormat();

        assertFalse(constraint.isViolated(new SlotDataValue.TextValue("user@example.com")));
        assertTrue(constraint.isViolated(new SlotDataValue.TextValue("invalid")));
    }

    @Test
    void validatesNumberRange() {
        SlotConstraint constraint = new SlotConstraint.NumberRange(1, 6);

        assertFalse(constraint.isViolated(new SlotDataValue.NumberValue(4)));
        assertTrue(constraint.isViolated(new SlotDataValue.NumberValue(7)));
    }

    @Test
    void validatesFutureDate() {
        SlotConstraint constraint = new SlotConstraint.FutureDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        assertFalse(constraint.isViolated(new SlotDataValue.DateValue(today)));
        assertTrue(constraint.isViolated(new SlotDataValue.DateValue(today.minusDays(1))));
    }
}
