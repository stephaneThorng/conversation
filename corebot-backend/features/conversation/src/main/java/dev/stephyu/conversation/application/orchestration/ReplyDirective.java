package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.domain.ConversationState;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ReplyDirective(
        ConversationState state,
        String messageKey,
        Map<String, String> arguments
) {

    public ReplyDirective {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(messageKey, "messageKey must not be null");
        arguments = Map.copyOf(arguments);
    }
}
