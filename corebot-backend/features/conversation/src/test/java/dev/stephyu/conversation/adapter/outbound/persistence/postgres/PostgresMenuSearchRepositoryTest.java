package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PostgresMenuSearchRepositoryTest extends PostgresMenuSearchRepositoryTestSupport {

    @Test
    void shouldSearchMenusByNameAndIngredient() {
        List<MenuSearchResult> results = repository.searchMenus(new MenuSearchQuery(ESTABLISHMENT_ID));

        assertEquals(1, results.size());
        MenuSearchResult menu = results.getFirst();
        assertEquals(BRUNCH_MENU_ID, menu.menuId());
        assertEquals(2, menu.sections().size());
        assertEquals("starter", menu.sections().getFirst().sectionCode());
        assertEquals(1590, menu.sections().getFirst().items().getFirst().priceCents());
    }

    @Test
    void shouldSearchMenuItemsByNameAllergenDietAndEstablishment() {
        assertEquals(
                List.of(PAPAYA_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(ESTABLISHMENT_ID))
                        .stream()
                        .filter(r -> r.menuItemId().equals(PAPAYA_ITEM_ID))
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());

        assertEquals(
                List.of(MISO_ITEM_ID, SPRITZ_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(ESTABLISHMENT_ID))
                        .stream()
                        .filter(r -> r.dietaryRestrictionCodes().contains("vegan"))
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());
    }
}
