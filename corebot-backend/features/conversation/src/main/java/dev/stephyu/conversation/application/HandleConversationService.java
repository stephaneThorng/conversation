package dev.stephyu.conversation.application;

import dev.stephyu.conversation.application.orchestration.ConversationOrchestrator;
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
    private final ConversationOrchestrator conversationOrchestrator;

    public HandleConversationService(
            ConversationStateRepositoryPort conversationStateRepository,
            ConversationOrchestrator conversationOrchestrator) {
        this.conversationStateRepository = conversationStateRepository;
        this.conversationOrchestrator = conversationOrchestrator;
    }


    @Override
    public HandleConversationResult handle(HandleConversationCommand command) {
        ConversationSession session = loadSession(command.sessionId(), command.establishmentId());
        LOGGER.debug(
                "Conversation request received: sessionId={}, establishmentId={}, existingWorkflow={}, recentTurnsCount={}, message={}",
                command.sessionId().value(),
                command.establishmentId().value(),
                session.state().activeWorkflow().map(workflow -> workflow.type().name()).orElse("none"),
                session.state().recentTurns().size(),
                command.message());
        ConversationSession sessionWithUserMessage = session.withState(session.state().withUserMessage(command.message()));
        ConversationOrchestrator.OrchestrationResult result = conversationOrchestrator.orchestrate(sessionWithUserMessage, command.message());
        saveSession(result.session());
        LOGGER.debug(
                "Conversation request handled: sessionId={}, reply={}, resultingWorkflow={}, recentTurnsCount={}",
                command.sessionId().value(),
                result.reply(),
                result.session().state().activeWorkflow().map(workflow -> workflow.type().name()).orElse("none"),
                result.session().state().recentTurns().size());
        return new HandleConversationResult(command.sessionId(), result.reply());
    }

    private ConversationSession loadSession(SessionId sessionId, dev.stephyu.conversation.domain.EstablishmentId establishmentId) {
        return conversationStateRepository.findBySessionId(sessionId)
                .orElseGet(() -> new ConversationSession(
                        sessionId,
                        new ConversationState(establishmentId, null)));
    }

    private void saveSession(ConversationSession session) {
        conversationStateRepository.save(session);
    }
}
