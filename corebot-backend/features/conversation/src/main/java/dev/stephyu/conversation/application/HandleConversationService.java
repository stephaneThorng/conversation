package dev.stephyu.conversation.application;

import dev.stephyu.conversation.application.port.outbound.ConversationAgentPort;
import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.SessionId;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class HandleConversationService implements HandleConversationUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleConversationService.class);

    private final ConversationStateRepositoryPort conversationStateRepository;
    private final ConversationAgentPort conversationAgentPort;

    public HandleConversationService(
            ConversationStateRepositoryPort conversationStateRepository,
            ConversationAgentPort conversationAgentPort) {
        this.conversationStateRepository = conversationStateRepository;
        this.conversationAgentPort = conversationAgentPort;
    }

    @Override
    public HandleConversationResult handle(HandleConversationCommand command) {
        ConversationSession session = loadSession(command.sessionId(), command.establishmentId());
        LOGGER.debug(
                "Conversation request received: sessionId={}, establishmentId={}, message={}",
                command.sessionId().value(),
                command.establishmentId().value(),
                command.message());

        String reply = conversationAgentPort.chat(
                command.sessionId().value(),
                command.establishmentId().value(),
                command.message());

        saveSession(session);
        LOGGER.debug(
                "Conversation request handled: sessionId={}, reply={}",
                command.sessionId().value(),
                reply);
        return new HandleConversationResult(command.sessionId(), reply);
    }

    private ConversationSession loadSession(SessionId sessionId, dev.stephyu.conversation.domain.EstablishmentId establishmentId) {
        return conversationStateRepository.findBySessionId(sessionId)
                .orElseGet(() -> new ConversationSession(
                        sessionId,
                        new ConversationState(establishmentId)));
    }

    private void saveSession(ConversationSession session) {
        conversationStateRepository.save(session);
    }
}
