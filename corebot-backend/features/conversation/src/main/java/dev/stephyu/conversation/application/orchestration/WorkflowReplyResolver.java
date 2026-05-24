package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort;
import dev.stephyu.conversation.application.reply.ResponseTone;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WorkflowReplyResolver {

    private final ConversationReplyPort conversationReplyPort;

    public WorkflowReplyResolver(ConversationReplyPort conversationReplyPort) {
        this.conversationReplyPort = Objects.requireNonNull(conversationReplyPort, "conversationReplyPort must not be null");
    }

    public HandlerResult resolve(ReplyDirective directive, String language, ResponseTone responseTone, String sessionId, String userMessage) {
        if (directive.directReply() != null) {
            return new HandlerResult(directive.state(), directive.directReply());
        }
        String reply = conversationReplyPort.reply(
                sessionId,
                language,
                new ConversationReplyPort.ReplyContext(directive.replyIntent(), directive.arguments()),
                userMessage);
        return new HandlerResult(directive.state(), reply);
    }
}
