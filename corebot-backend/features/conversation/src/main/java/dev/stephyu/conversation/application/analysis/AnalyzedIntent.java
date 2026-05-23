package dev.stephyu.conversation.application.analysis;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AnalyzedIntent(
        AnalyzedIntentName name,
        List<AnalyzedEntity> entities
) {

    public AnalyzedIntent {
        Objects.requireNonNull(name, "name must not be null");
        entities = List.copyOf(entities);
    }
}
