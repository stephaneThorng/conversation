package dev.stephyu.conversation.domain.slot;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SlotDefinition(
        SlotName name,
        boolean required,
        List<SlotConstraint> constraints
) {

    public SlotDefinition {
        Objects.requireNonNull(name, "name must not be null");
        constraints = List.copyOf(constraints);
    }

    public SlotDataType dataType() {
        return name.dataType();
    }
}
