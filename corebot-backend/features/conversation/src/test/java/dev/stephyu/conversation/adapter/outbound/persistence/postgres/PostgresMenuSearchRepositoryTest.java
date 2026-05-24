package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import dev.stephyu.conversation.domain.menu.PriceComparator;
import dev.stephyu.conversation.domain.menu.PriceCriterion;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PostgresMenuSearchRepositoryTest extends PostgresMenuSearchRepositoryTestSupport {

    @Test
    void shouldSearchMenusByNameIngredientAndOverridePrice() {
        List<MenuSearchResult> results = repository.searchMenus(new MenuSearchQuery(
                ESTABLISHMENT_ID,
                "BRUNCH",
                "lime",
                null));

        assertEquals(1, results.size());
        MenuSearchResult menu = results.getFirst();
        assertEquals(BRUNCH_MENU_ID, menu.menuId());
        assertEquals(2, menu.sections().size());
        assertEquals("starter", menu.sections().getFirst().sectionCode());
        assertEquals(1590, menu.sections().getFirst().items().getFirst().priceCents());
    }

    @Test
    void shouldApplyAllMenuPriceComparators() {
        assertEquals(
                List.of("tasting_menu"),
                repository.searchMenus(new MenuSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                new PriceCriterion(PriceComparator.GREATER_THAN, 3000, null)))
                        .stream()
                        .map(MenuSearchResult::menuCode)
                        .toList());

        assertEquals(
                List.of("drinks_menu"),
                repository.searchMenus(new MenuSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                new PriceCriterion(PriceComparator.LESSER_THAN, 2000, null)))
                        .stream()
                        .map(MenuSearchResult::menuCode)
                        .toList());

        assertEquals(
                List.of("brunch_menu"),
                repository.searchMenus(new MenuSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                new PriceCriterion(PriceComparator.EQUAL, 2500, null)))
                        .stream()
                        .map(MenuSearchResult::menuCode)
                        .toList());

        assertEquals(
                List.of("brunch_menu"),
                repository.searchMenus(new MenuSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                new PriceCriterion(PriceComparator.BETWEEN, 2000, 3000)))
                        .stream()
                        .map(MenuSearchResult::menuCode)
                        .toList());
    }

    @Test
    void shouldSearchMenuItemsByNameAllergenDietPriceAndEstablishment() {
        assertEquals(
                List.of(PAPAYA_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(
                                ESTABLISHMENT_ID,
                                "PAPAYA",
                                null,
                                null,
                                null))
                        .stream()
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());

        assertEquals(
                List.of(PAPAYA_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                "nut",
                                null,
                                null))
                        .stream()
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());

        assertEquals(
                List.of(MISO_ITEM_ID, SPRITZ_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                "vegan",
                                null))
                        .stream()
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());

        assertEquals(
                List.of(WAGYU_ITEM_ID),
                repository.searchMenuItems(new MenuItemSearchQuery(
                                ESTABLISHMENT_ID,
                                null,
                                null,
                                null,
                                new PriceCriterion(PriceComparator.GREATER_THAN, 2000, null)))
                        .stream()
                        .map(MenuItemSearchResult::menuItemId)
                        .toList());
    }
}
