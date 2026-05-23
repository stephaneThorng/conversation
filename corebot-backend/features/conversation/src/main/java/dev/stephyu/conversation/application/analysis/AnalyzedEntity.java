package dev.stephyu.conversation.application.analysis;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AnalyzedEntity(
        AnalyzedEntityType type,
        String rawValue
) {

    public AnalyzedEntity {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(rawValue, "rawValue must not be null");
    }
}
