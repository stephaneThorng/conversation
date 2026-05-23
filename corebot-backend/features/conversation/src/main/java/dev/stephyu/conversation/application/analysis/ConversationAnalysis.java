package dev.stephyu.conversation.application.analysis;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConversationAnalysis(
        @Nullable String language,
        List<AnalyzedIntent> intents
) {

    public ConversationAnalysis {
        intents = List.copyOf(intents);
    }

    public Optional<AnalyzedIntent> firstIntent() {
        return intents.stream().findFirst();
    }
}
