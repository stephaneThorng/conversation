package dev.stephyu.conversation.domain.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface SlotDataValue
        permits SlotDataValue.TextValue,
                SlotDataValue.DateValue,
                SlotDataValue.TimeValue,
                SlotDataValue.NumberValue,
                SlotDataValue.BooleanValue {

    SlotDataType type();

    record TextValue(String value) implements SlotDataValue {
        @Override
        public SlotDataType type() {
            return SlotDataType.TEXT;
        }
    }

    record DateValue(LocalDate value) implements SlotDataValue {
        @Override
        public SlotDataType type() {
            return SlotDataType.DATE;
        }
    }

    record TimeValue(LocalTime value) implements SlotDataValue {
        @Override
        public SlotDataType type() {
            return SlotDataType.TIME;
        }
    }

    record NumberValue(int value) implements SlotDataValue {
        @Override
        public SlotDataType type() {
            return SlotDataType.NUMBER;
        }
    }

    record BooleanValue(boolean value) implements SlotDataValue {
        @Override
        public SlotDataType type() {
            return SlotDataType.BOOLEAN;
        }
    }
}
