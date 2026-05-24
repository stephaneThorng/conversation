package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort.ReplyIntent;
import dev.stephyu.conversation.domain.ConversationState;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ReplyDirective(
        ConversationState state,
        ReplyIntent replyIntent,
        Map<String, String> arguments,
        @Nullable String directReply
) {

    public ReplyDirective {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(replyIntent, "replyIntent must not be null");
        arguments = Map.copyOf(arguments);
    }

    public ReplyDirective(ConversationState state, ReplyIntent replyIntent, Map<String, String> arguments) {
        this(state, replyIntent, arguments, null);
    }
}
