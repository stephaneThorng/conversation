package dev.stephyu.conversation.application.analysis;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConversationAnalysis(
        @Nullable String language,
        List<AnalyzedIntent> intents,
        boolean affirmative,
        boolean negative
) {

    public ConversationAnalysis {
        intents = List.copyOf(intents);
    }

    /** Convenience constructor for tests / cases without control signals. */
    public ConversationAnalysis(@Nullable String language, List<AnalyzedIntent> intents) {
        this(language, intents, false, false);
    }

    public Optional<AnalyzedIntent> firstIntent() {
        return intents.stream().findFirst();
    }
}
