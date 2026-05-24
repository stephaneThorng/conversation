package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_ITEM;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_SECTION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_SECTION_ITEM_MAP;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import dev.stephyu.conversation.domain.menu.PriceCriterion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PostgresMenuSearchRepository implements SearchMenuRepositoryPort {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() { };

    private final DSLContext dsl;

    public PostgresMenuSearchRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    @Override
    public List<MenuSearchResult> searchMenus(MenuSearchQuery query) {
        var menuRecords = dsl.select(
                        RESTAURANT_MENU.ID,
                        RESTAURANT_MENU.CODE,
                        RESTAURANT_MENU.NAME_TRANSLATIONS,
                        RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS,
                        RESTAURANT_MENU.PRICE_CENTS,
                        RESTAURANT_MENU.CURRENCY)
                .from(RESTAURANT_MENU)
                .where(buildMenuConditions(query))
                .orderBy(RESTAURANT_MENU.SORT_ORDER.asc(), RESTAURANT_MENU.CODE.asc())
                .fetch();

        if (menuRecords.isEmpty()) {
            return List.of();
        }

        List<UUID> menuIds = menuRecords.stream()
                .map(record -> record.get(RESTAURANT_MENU.ID))
                .toList();

        Map<UUID, List<MenuSearchResult.MenuSectionResult>> sectionsByMenuId = loadSectionsByMenuId(menuIds);
        List<MenuSearchResult> results = new ArrayList<>(menuRecords.size());
        for (Record record : menuRecords) {
            UUID menuId = record.get(RESTAURANT_MENU.ID);
            results.add(new MenuSearchResult(
                    menuId,
                    record.get(RESTAURANT_MENU.CODE),
                    translations(record.get(RESTAURANT_MENU.NAME_TRANSLATIONS)),
                    translations(record.get(RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS)),
                    record.get(RESTAURANT_MENU.PRICE_CENTS),
                    record.get(RESTAURANT_MENU.CURRENCY),
                    sectionsByMenuId.getOrDefault(menuId, List.of())));
        }
        return List.copyOf(results);
    }

    @Override
    public List<MenuItemSearchResult> searchMenuItems(MenuItemSearchQuery query) {
        return dsl.select(
                        RESTAURANT_MENU_ITEM.ID,
                        RESTAURANT_MENU_ITEM.CODE,
                        RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.PRICE_CENTS,
                        RESTAURANT_MENU_ITEM.CURRENCY,
                        RESTAURANT_MENU_ITEM.CATEGORY_CODES,
                        RESTAURANT_MENU_ITEM.ALLERGEN_CODES,
                        RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES)
                .from(RESTAURANT_MENU_ITEM)
                .where(buildMenuItemConditions(query))
                .orderBy(RESTAURANT_MENU_ITEM.CODE.asc())
                .fetch(record -> new MenuItemSearchResult(
                        record.get(RESTAURANT_MENU_ITEM.ID),
                        record.get(RESTAURANT_MENU_ITEM.CODE),
                        translations(record.get(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS)),
                        translations(record.get(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS)),
                        translations(record.get(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS)),
                        record.get(RESTAURANT_MENU_ITEM.PRICE_CENTS),
                        record.get(RESTAURANT_MENU_ITEM.CURRENCY),
                        arrayValues(record.get(RESTAURANT_MENU_ITEM.CATEGORY_CODES)),
                        arrayValues(record.get(RESTAURANT_MENU_ITEM.ALLERGEN_CODES)),
                        arrayValues(record.get(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES))));
    }

    private Map<UUID, List<MenuSearchResult.MenuSectionResult>> loadSectionsByMenuId(List<UUID> menuIds) {
        var sectionRecords = dsl.select(
                        RESTAURANT_MENU_SECTION.ID,
                        RESTAURANT_MENU_SECTION.MENU_ID,
                        RESTAURANT_MENU_SECTION.CODE,
                        RESTAURANT_MENU_SECTION.NAME_TRANSLATIONS,
                        RESTAURANT_MENU_SECTION.DESCRIPTION_TRANSLATIONS)
                .from(RESTAURANT_MENU_SECTION)
                .where(RESTAURANT_MENU_SECTION.MENU_ID.in(menuIds))
                .and(RESTAURANT_MENU_SECTION.ACTIVE.isTrue())
                .orderBy(RESTAURANT_MENU_SECTION.SORT_ORDER.asc(), RESTAURANT_MENU_SECTION.CODE.asc())
                .fetch();

        Map<UUID, List<MenuSearchResult.MenuCompositionItemResult>> itemsBySectionId = loadItemsBySectionId(menuIds);
        Map<UUID, List<MenuSearchResult.MenuSectionResult>> sectionsByMenuId = new LinkedHashMap<>();
        for (Record record : sectionRecords) {
            UUID menuId = record.get(RESTAURANT_MENU_SECTION.MENU_ID);
            UUID sectionId = record.get(RESTAURANT_MENU_SECTION.ID);
            MenuSearchResult.MenuSectionResult section = new MenuSearchResult.MenuSectionResult(
                    sectionId,
                    record.get(RESTAURANT_MENU_SECTION.CODE),
                    translations(record.get(RESTAURANT_MENU_SECTION.NAME_TRANSLATIONS)),
                    translations(record.get(RESTAURANT_MENU_SECTION.DESCRIPTION_TRANSLATIONS)),
                    itemsBySectionId.getOrDefault(sectionId, List.of()));
            sectionsByMenuId.computeIfAbsent(menuId, ignored -> new ArrayList<>()).add(section);
        }
        return copyNestedLists(sectionsByMenuId);
    }

    private Map<UUID, List<MenuSearchResult.MenuCompositionItemResult>> loadItemsBySectionId(List<UUID> menuIds) {
        var itemRecords = dsl.select(
                        RESTAURANT_MENU_SECTION.ID.as("section_id"),
                        RESTAURANT_MENU_ITEM.ID,
                        RESTAURANT_MENU_ITEM.CODE,
                        RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS,
                        RESTAURANT_MENU_ITEM.PRICE_CENTS,
                        RESTAURANT_MENU_ITEM.CURRENCY,
                        RESTAURANT_MENU_ITEM.CATEGORY_CODES,
                        RESTAURANT_MENU_ITEM.ALLERGEN_CODES,
                        RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES,
                        RESTAURANT_MENU_SECTION_ITEM_MAP.PRICE_CENTS_OVERRIDE)
                .from(RESTAURANT_MENU_SECTION)
                .join(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .on(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID.eq(RESTAURANT_MENU_SECTION.ID))
                .join(RESTAURANT_MENU_ITEM)
                .on(RESTAURANT_MENU_ITEM.ID.eq(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID))
                .where(RESTAURANT_MENU_SECTION.MENU_ID.in(menuIds))
                .and(RESTAURANT_MENU_SECTION.ACTIVE.isTrue())
                .and(RESTAURANT_MENU_ITEM.ACTIVE.isTrue())
                .orderBy(RESTAURANT_MENU_SECTION.SORT_ORDER.asc(),
                        RESTAURANT_MENU_SECTION_ITEM_MAP.SORT_ORDER.asc(),
                        RESTAURANT_MENU_ITEM.CODE.asc())
                .fetch();

        Field<UUID> sectionIdField = DSL.field("section_id", UUID.class);
        Map<UUID, List<MenuSearchResult.MenuCompositionItemResult>> itemsBySectionId = new LinkedHashMap<>();
        for (Record record : itemRecords) {
            UUID sectionId = record.get(sectionIdField);
            Integer overridePrice = record.get(RESTAURANT_MENU_SECTION_ITEM_MAP.PRICE_CENTS_OVERRIDE);
            int effectivePrice = overridePrice != null
                    ? overridePrice
                    : Optional.ofNullable(record.get(RESTAURANT_MENU_ITEM.PRICE_CENTS)).orElseThrow();
            MenuSearchResult.MenuCompositionItemResult item = new MenuSearchResult.MenuCompositionItemResult(
                    record.get(RESTAURANT_MENU_ITEM.ID),
                    record.get(RESTAURANT_MENU_ITEM.CODE),
                    translations(record.get(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS)),
                    translations(record.get(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS)),
                    translations(record.get(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS)),
                    effectivePrice,
                    record.get(RESTAURANT_MENU_ITEM.CURRENCY),
                    arrayValues(record.get(RESTAURANT_MENU_ITEM.CATEGORY_CODES)),
                    arrayValues(record.get(RESTAURANT_MENU_ITEM.ALLERGEN_CODES)),
                    arrayValues(record.get(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES)));
            itemsBySectionId.computeIfAbsent(sectionId, ignored -> new ArrayList<>()).add(item);
        }
        return copyNestedLists(itemsBySectionId);
    }

    private Condition buildMenuConditions(MenuSearchQuery query) {
        Condition condition = RESTAURANT_MENU.ESTABLISHMENT_ID.eq(query.establishmentId())
                .and(RESTAURANT_MENU.ACTIVE.isTrue());
        if (query.menuNameFilter().isPresent()) {
            condition = condition.and(jsonTextContains(
                    RESTAURANT_MENU.NAME_TRANSLATIONS,
                    query.menuNameFilter().orElseThrow()));
        }
        if (query.ingredientFilter().isPresent()) {
            condition = condition.and(menuContainsIngredient(query.ingredientFilter().orElseThrow()));
        }
        condition = withPriceCriterion(condition, RESTAURANT_MENU.PRICE_CENTS, query.priceCriterion());
        return condition;
    }

    private Condition buildMenuItemConditions(MenuItemSearchQuery query) {
        Condition condition = RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID.eq(query.establishmentId())
                .and(RESTAURANT_MENU_ITEM.ACTIVE.isTrue());
        if (query.itemNameFilter().isPresent()) {
            condition = condition.and(jsonTextContains(
                    RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS,
                    query.itemNameFilter().orElseThrow()));
        }
        if (query.ingredientFilter().isPresent()) {
            condition = condition.and(jsonTextContains(
                    RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS,
                    query.ingredientFilter().orElseThrow()));
        }
        if (query.allergenCodeFilter().isPresent()) {
            condition = condition.and(arrayContains(
                    RESTAURANT_MENU_ITEM.ALLERGEN_CODES,
                    query.allergenCodeFilter().orElseThrow()));
        }
        if (query.dietaryRestrictionCodeFilter().isPresent()) {
            condition = condition.and(arrayContains(
                    RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES,
                    query.dietaryRestrictionCodeFilter().orElseThrow()));
        }
        condition = withPriceCriterion(condition, RESTAURANT_MENU_ITEM.PRICE_CENTS, query.priceCriterion());
        return condition;
    }

    private Condition menuContainsIngredient(String ingredient) {
        return DSL.exists(DSL.selectOne()
                .from(RESTAURANT_MENU_SECTION)
                .join(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .on(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID.eq(RESTAURANT_MENU_SECTION.ID))
                .join(RESTAURANT_MENU_ITEM)
                .on(RESTAURANT_MENU_ITEM.ID.eq(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID))
                .where(RESTAURANT_MENU_SECTION.MENU_ID.eq(RESTAURANT_MENU.ID))
                .and(RESTAURANT_MENU_SECTION.ACTIVE.isTrue())
                .and(RESTAURANT_MENU_ITEM.ACTIVE.isTrue())
                .and(jsonTextContains(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, ingredient)));
    }

    private static Condition withPriceCriterion(
            Condition condition,
            Field<Integer> field,
            @Nullable PriceCriterion priceCriterion) {
        if (priceCriterion == null) {
            return condition;
        }
        int minPrice = Optional.ofNullable(priceCriterion.minPriceCents()).orElseThrow();
        return switch (priceCriterion.comparator()) {
            case GREATER_THAN -> condition.and(field.gt(minPrice));
            case LESSER_THAN -> condition.and(field.lt(minPrice));
            case EQUAL -> condition.and(field.eq(minPrice));
            case BETWEEN -> condition.and(field.between(
                    minPrice,
                    Optional.ofNullable(priceCriterion.maxPriceCents()).orElseThrow()));
        };
    }

    private static Condition jsonTextContains(Field<JSON> field, String value) {
        String pattern = "%" + value.toLowerCase(java.util.Locale.ROOT) + "%";
        return DSL.condition("lower(cast({0} as text)) like {1}", field, pattern);
    }

    private static Condition arrayContains(Field<String[]> field, String value) {
        return DSL.condition("{0} @> {1}::text[]", field, DSL.val(new String[] { value }));
    }

    private static List<String> arrayValues(@Nullable String[] values) {
        return values == null ? List.of() : List.of(values);
    }

    private static Map<String, String> translations(@Nullable JSON json) {
        if (json == null || json.data().isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json.data(), STRING_MAP);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decode JSON translations", exception);
        }
    }

    private static <K, V> Map<K, List<V>> copyNestedLists(Map<K, List<V>> source) {
        Map<K, List<V>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }
}
