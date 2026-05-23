package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stephyu.conversation.adapter.outbound.reply.PropertiesConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowReplyResolverTest {

    @Test
    void resolvesReplyDirectiveWithLanguageToneAndArguments() {
        WorkflowReplyResolver resolver = new WorkflowReplyResolver(new PropertiesConversationReplyCatalog());
        ReplyDirective directive = new ReplyDirective(
                new ConversationState(EstablishmentId.of("est-1"), null),
                "reservation_create.success",
                Map.of(
                        "reservation_name", "Martin",
                        "date", "2026-05-24",
                        "time", "18:00",
                        "people_count", "4"));

        HandlerResult result = resolver.resolve(directive, "en", ResponseTone.FRIENDLY);

        assertEquals(
                "Your reservation is confirmed for Martin on 2026-05-24 at 18:00 for 4 people.",
                result.reply());
    }
}
