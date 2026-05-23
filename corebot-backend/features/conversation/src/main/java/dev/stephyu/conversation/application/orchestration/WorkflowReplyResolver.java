package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.reply.ConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.ResponseTone;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WorkflowReplyResolver {

    private final ConversationReplyCatalog replyCatalog;

    public WorkflowReplyResolver(ConversationReplyCatalog replyCatalog) {
        this.replyCatalog = Objects.requireNonNull(replyCatalog, "replyCatalog must not be null");
    }

    public HandlerResult resolve(ReplyDirective directive, String language, ResponseTone responseTone) {
        String reply = replyCatalog.resolve(language, responseTone, directive.messageKey(), directive.arguments());
        return new HandlerResult(directive.state(), reply);
    }
}
