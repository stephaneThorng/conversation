package dev.stephyu.conversation.application;

import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.LlmAssistantPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.SessionId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class HandleConversationService implements HandleConversationUseCase {

    private final LlmAssistantPort llmAssistantPort;
    private final ConversationStateRepositoryPort conversationStateRepository;

    public HandleConversationService(
            LlmAssistantPort llmAssistantPort,
            ConversationStateRepositoryPort conversationStateRepository) {
        this.llmAssistantPort = llmAssistantPort;
        this.conversationStateRepository = conversationStateRepository;
    }


    @Override
    public HandleConversationResult handle(HandleConversationCommand command) {
        ConversationSession session = loadSession(command.sessionId(), command.establishmentId());
        String answer = llmAssistantPort.chat(command.message());
        saveSession(session, answer);
        return new HandleConversationResult(command.sessionId(), answer);
    }

    private ConversationSession loadSession(SessionId sessionId, dev.stephyu.conversation.domain.EstablishmentId establishmentId) {
        return conversationStateRepository.findBySessionId(sessionId)
                .orElseGet(() -> new ConversationSession(
                        sessionId,
                        new ConversationState(establishmentId, null)));
    }

    private void saveSession(ConversationSession session, String answer) {
        ConversationSession updatedSession = new ConversationSession(
                session.sessionId(),
                session.state().withLastAssistantReply(answer));
        conversationStateRepository.save(updatedSession);
    }
}
