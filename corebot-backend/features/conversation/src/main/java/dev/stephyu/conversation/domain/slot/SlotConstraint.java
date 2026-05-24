package dev.stephyu.conversation.domain.slot;

import java.time.LocalDate;
import java.time.ZoneId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface SlotConstraint {

    boolean isViolated(SlotDataValue value);

    record TextMaxLen(int maxLength) implements SlotConstraint {

        public TextMaxLen {
            if (maxLength < 0) {
                throw new IllegalArgumentException("maxLength must be positive");
            }
        }

        @Override
        public boolean isViolated(SlotDataValue value) {
            return value instanceof SlotDataValue.TextValue(String text) && text.length() > maxLength;
        }
    }

    record EmailFormat() implements SlotConstraint {

        @Override
        public boolean isViolated(SlotDataValue value) {
            if (!(value instanceof SlotDataValue.TextValue(String text))) {
                return false;
            }
            int separator = text.indexOf('@');
            return !(separator > 0
                    && separator == text.lastIndexOf('@')
                    && separator < text.length() - 1
                    && text.substring(separator + 1).contains("."));
        }
    }

    record NumberRange(int min, int max) implements SlotConstraint {

        public NumberRange {
            if (min > max) {
                throw new IllegalArgumentException("min must be lower than or equal to max");
            }
        }

        @Override
        public boolean isViolated(SlotDataValue value) {
            return value instanceof SlotDataValue.NumberValue(int number)
                    && (number < min || number > max);
        }
    }

    record FutureDate() implements SlotConstraint {

        @Override
        public boolean isViolated(SlotDataValue value) {
            return value instanceof SlotDataValue.DateValue(LocalDate date)
                    && date.isBefore(LocalDate.now(ZoneId.systemDefault()));
        }
    }
}
