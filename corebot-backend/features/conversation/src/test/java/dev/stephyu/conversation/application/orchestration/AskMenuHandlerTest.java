package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.MenuAssistantPort;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.workflow.Workflow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AskMenuHandlerTest {

    @Test
    void shouldDelegateToMenuAssistantPort() {
        UUID establishmentId = UUID.fromString("6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101");
        MenuAssistantPort menuAssistantPort = (sessionId, userMessage, language, estId) ->
                "A La Carte: Starters - Green Papaya Salad";

        AskMenuHandler handler = new AskMenuHandler(menuAssistantPort);
        Workflow workflow = handler.newWorkflow();

        WorkflowPostProcessResult result = handler.onConfirmed(handlerInput(establishmentId), workflow);

        assertTrue(result.success());
        assertTrue(result.arguments().get("results").contains("A La Carte"));
        assertTrue(result.arguments().get("results").contains("Green Papaya Salad"));
    }

    private static HandlerInput handlerInput(UUID establishmentId) {
        return new HandlerInput(
                new ConversationSession(
                        SessionId.of("session-1"),
                        new ConversationState(EstablishmentId.of(establishmentId.toString()), null)),
                "show me the menu",
                List.of(),
                false,
                false,
                "en",
                ResponseTone.FRIENDLY);
    }
}
