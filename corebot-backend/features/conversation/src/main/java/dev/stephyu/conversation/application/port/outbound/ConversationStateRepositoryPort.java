package dev.stephyu.conversation.application.port.outbound;

import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.SessionId;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConversationStateRepositoryPort {

    Optional<ConversationSession> findBySessionId(SessionId sessionId);

    void save(ConversationSession session);
}
