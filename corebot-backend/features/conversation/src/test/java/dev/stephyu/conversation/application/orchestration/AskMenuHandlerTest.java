package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AskMenuHandlerTest {

    @Test
    void shouldSearchMenusAndFormatSections() {
        UUID establishmentId = UUID.fromString("6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101");
        SearchMenuRepositoryPort repositoryPort = new SearchMenuRepositoryPort() {
            @Override
            public List<MenuSearchResult> searchMenus(MenuSearchQuery query) {
                assertEquals(establishmentId, query.establishmentId());
                assertEquals("a la carte", query.menuName());
                return List.of(new MenuSearchResult(
                        UUID.randomUUID(),
                        "a_la_carte",
                        Map.of("en", "A La Carte"),
                        Map.of(),
                        null,
                        "EUR",
                        List.of(new MenuSearchResult.MenuSectionResult(
                                UUID.randomUUID(),
                                "starter",
                                Map.of("en", "Starters"),
                                Map.of(),
                                List.of(new MenuSearchResult.MenuCompositionItemResult(
                                        UUID.randomUUID(),
                                        "green_papaya_salad",
                                        Map.of("en", "Green Papaya Salad"),
                                        Map.of(),
                                        Map.of(),
                                        300,
                                        "EUR",
                                        List.of(),
                                        List.of(),
                                        List.of()))))));
            }

            @Override
            public List<MenuItemSearchResult> searchMenuItems(MenuItemSearchQuery query) {
                return List.of();
            }
        };

        AskMenuHandler handler = new AskMenuHandler(repositoryPort);
        Workflow workflow = handler.newWorkflow().withCollectedData(
                CollectedData.empty().withValue(SlotName.MENU_NAME, new SlotDataValue.TextValue("a la carte")));

        WorkflowPostProcessResult result = handler.onConfirmed(handlerInput(establishmentId), workflow);

        assertEquals(true, result.success());
        assertEquals("ask_menu.found", result.messageKey());
        assertEquals(true, result.arguments().get("results").contains("A La Carte"));
        assertEquals(true, result.arguments().get("results").contains("Green Papaya Salad"));
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
