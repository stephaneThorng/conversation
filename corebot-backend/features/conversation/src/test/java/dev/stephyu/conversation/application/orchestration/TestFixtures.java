package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import java.util.List;

final class TestFixtures {

    private TestFixtures() {
    }

    static HandlerInput handlerInput() {
        return new HandlerInput(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "hello",
                List.of(),
                false,
                false,
                "en",
                ResponseTone.FRIENDLY);
    }
}
