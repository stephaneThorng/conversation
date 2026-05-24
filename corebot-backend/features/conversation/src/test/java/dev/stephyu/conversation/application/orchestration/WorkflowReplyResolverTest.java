package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowReplyResolverTest {

    // Stub that echoes the intent + facts as the reply — avoids real LLM calls in unit tests.
    private static final ConversationReplyPort STUB_REPLY_PORT =
            (sessionId, language, context, userMessage) ->
                    context.replyIntent().name() + ": " + context.facts().toString();

    @Test
    void resolvesReplyDirectiveToNonEmptyReply() {
        WorkflowReplyResolver resolver = new WorkflowReplyResolver(STUB_REPLY_PORT);
        ReplyDirective directive = new ReplyDirective(
                new ConversationState(EstablishmentId.of("est-1"), null),
                ConversationReplyPort.ReplyIntent.WORKFLOW_SUCCESS,
                Map.of(
                        "reservation_name", "Martin",
                        "date", "2026-05-24",
                        "time", "18:00",
                        "people_count", "4",
                        "reference", "ABC12345"));

        HandlerResult result = resolver.resolve(directive, "en", ResponseTone.FRIENDLY, "session-1", "confirm please");

        assertNotNull(result.reply());
        assertFalse(result.reply().isBlank());
    }
}
