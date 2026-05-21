package dev.stephyu.conversation.domain.slot;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CollectedData(Map<SlotName, SlotDataValue> values) {

    public CollectedData {
        values = Map.copyOf(values);
    }

    public static CollectedData empty() {
        return new CollectedData(Map.of());
    }

    public Optional<SlotDataValue> value(SlotName name) {
        return Optional.ofNullable(values.get(name));
    }

    public <T extends SlotDataValue> Optional<T> valueAs(SlotName name, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return value(name).filter(type::isInstance).map(type::cast);
    }

    public boolean contains(SlotName name) {
        return values.containsKey(name);
    }

    public CollectedData withValue(SlotName name, SlotDataValue value) {
        if (name.dataType() != value.type()) {
            throw new IllegalArgumentException("slot value type does not match slot data type");
        }
        var nextValues = new java.util.HashMap<>(values);
        nextValues.put(name, value);
        return new CollectedData(nextValues);
    }

    public CollectedData withoutValue(SlotName name) {
        var nextValues = new java.util.HashMap<>(values);
        nextValues.remove(name);
        return new CollectedData(nextValues);
    }
}
