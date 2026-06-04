package dev.stephyu.conversation.adapter.outbound.persistence;

import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.SessionId;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class InMemoryConversationStateRepository implements ConversationStateRepositoryPort {

    private final ConcurrentMap<SessionId, ConversationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<ConversationSession> findBySessionId(SessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(ConversationSession session) {
        sessions.put(session.sessionId(), session);
    }
}
