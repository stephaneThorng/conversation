package dev.stephyu.conversation.domain.slot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface SlotConstraint {

    Optional<String> violationKey(SlotDataValue value);

    String defaultErrorKey();

    record TextMaxLen(int maxLength) implements SlotConstraint {

        public TextMaxLen {
            if (maxLength < 0) {
                throw new IllegalArgumentException("maxLength must be positive");
            }
        }

        @Override
        public Optional<String> violationKey(SlotDataValue value) {
            if (value instanceof SlotDataValue.TextValue(String text) && text.length() > maxLength) {
                return Optional.of(defaultErrorKey());
            }
            return Optional.empty();
        }

        @Override
        public String defaultErrorKey() {
            return "system.constraint.text_max_len.error";
        }
    }

    record EmailFormat() implements SlotConstraint {

        @Override
        public Optional<String> violationKey(SlotDataValue value) {
            if (!(value instanceof SlotDataValue.TextValue(String text))) {
                return Optional.empty();
            }
            int separator = text.indexOf('@');
            boolean valid = separator > 0
                    && separator == text.lastIndexOf('@')
                    && separator < text.length() - 1
                    && text.substring(separator + 1).contains(".");
            return valid ? Optional.empty() : Optional.of(defaultErrorKey());
        }

        @Override
        public String defaultErrorKey() {
            return "system.constraint.email_format.error";
        }
    }

    record NumberRange(int min, int max) implements SlotConstraint {

        public NumberRange {
            if (min > max) {
                throw new IllegalArgumentException("min must be lower than or equal to max");
            }
        }

        @Override
        public Optional<String> violationKey(SlotDataValue value) {
            if (value instanceof SlotDataValue.NumberValue(int number)
                    && (number < min || number > max)) {
                return Optional.of(defaultErrorKey());
            }
            return Optional.empty();
        }

        @Override
        public String defaultErrorKey() {
            return "system.constraint.number_range.error";
        }
    }

    record FutureDate() implements SlotConstraint {

        @Override
        public Optional<String> violationKey(SlotDataValue value) {
            if (value instanceof SlotDataValue.DateValue(LocalDate date)
                    && date.isBefore(LocalDate.now(ZoneId.systemDefault()))) {
                return Optional.of(defaultErrorKey());
            }
            return Optional.empty();
        }

        @Override
        public String defaultErrorKey() {
            return "system.constraint.future_date.error";
        }
    }
}
