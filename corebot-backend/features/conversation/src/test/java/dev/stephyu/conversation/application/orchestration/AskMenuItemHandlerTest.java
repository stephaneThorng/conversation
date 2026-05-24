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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AskMenuItemHandlerTest {

    @Test
    void shouldSearchMenuItemsAndFormatDetails() {
        UUID establishmentId = UUID.fromString("6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101");
        SearchMenuRepositoryPort repositoryPort = new SearchMenuRepositoryPort() {
            @Override
            public List<MenuSearchResult> searchMenus(MenuSearchQuery query) {
                return List.of();
            }

            @Override
            public List<MenuItemSearchResult> searchMenuItems(MenuItemSearchQuery query) {
                assertEquals(establishmentId, query.establishmentId());
                assertEquals("vegan", query.dietaryRestrictionCode());
                return List.of(new MenuItemSearchResult(
                        UUID.randomUUID(),
                        "yellow_tofu_curry",
                        Map.of("en", "Yellow Tofu Curry"),
                        Map.of(),
                        Map.of("en", "Tofu, coconut milk, turmeric"),
                        600,
                        "EUR",
                        List.of("main_course"),
                        List.of("soy"),
                        List.of("vegan", "gluten_free")));
            }
        };

        AskMenuItemHandler handler = new AskMenuItemHandler(repositoryPort);
        var workflow = handler.newWorkflow().withCollectedData(
                CollectedData.empty().withValue(
                        SlotName.MENU_DIETARY_RESTRICTION_CODE,
                        new SlotDataValue.TextValue("vegan")));

        WorkflowPostProcessResult result = handler.onConfirmed(handlerInput(establishmentId), workflow);

        assertEquals(true, result.success());
        assertEquals("ask_menu_item.found", result.messageKey());
        assertEquals(true, result.arguments().get("results").contains("Yellow Tofu Curry"));
        assertEquals(true, result.arguments().get("results").contains("vegan"));
    }

    private static HandlerInput handlerInput(UUID establishmentId) {
        return new HandlerInput(
                new ConversationSession(
                        SessionId.of("session-1"),
                        new ConversationState(EstablishmentId.of(establishmentId.toString()), null)),
                "show vegan dishes",
                List.of(),
                false,
                false,
                "en",
                ResponseTone.FRIENDLY);
    }
}
