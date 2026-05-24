package dev.stephyu.conversation.adapter.outbound.llm;
import dev.langchain4j.agent.tool.Tool;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Provides tool methods for the menu assistant LLM.
 * Results are serialized in a compact pipe-separated text format to minimize token consumption
 * compared to verbose JSON (~40 tokens/item vs ~100 tokens/item).
 *
 * Format for items:
 *   name | price | ingredients | allergens (comma-sep) | dietary (comma-sep)
 *
 * Format for menus:
 *   name [code] price
 *     Section: item1, item2
 */
@NullMarked
public final class MenuSearchToolProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuSearchToolProvider.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final SearchMenuRepositoryPort repository;
    private final UUID establishmentId;
    private final String language;
    // Lazily loaded, cached per establishment, refreshed after TTL expires.
    private @Nullable String cachedMenus;
    private @Nullable Instant cachedMenusAt;
    private @Nullable String cachedMenuItems;
    private @Nullable Instant cachedMenuItemsAt;
    public MenuSearchToolProvider(
            SearchMenuRepositoryPort repository,
            UUID establishmentId,
            String language) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId must not be null");
        this.language = Objects.requireNonNull(language, "language must not be null");
    }
    @Tool("Returns all active menus of the establishment, including their sections and items. " +
            "Use this when the user asks about menus, set menus, meal deals, or the overall menu structure. " +
            "Filter and select relevant results yourself based on the user question.")
    public synchronized String getAllMenus() {
        if (cachedMenus == null || isExpired(cachedMenusAt)) {
            LOGGER.debug("Tool getAllMenus: loading from DB for establishmentId={}", establishmentId);
            List<MenuSearchResult> results = repository.searchMenus(
                    new MenuSearchQuery(establishmentId));
            cachedMenus = formatMenus(results);
            cachedMenusAt = Instant.now();
            LOGGER.debug("Tool getAllMenus: loaded {} menus, {} chars", results.size(), cachedMenus.length());
        } else {
            LOGGER.debug("Tool getAllMenus: returning cached menus ({} chars)", cachedMenus.length());
        }
        return cachedMenus;
    }
    @Tool("Returns all active dishes/items of the establishment, including name, price, ingredients, allergens and dietary labels. " +
            "Use this when the user asks about dishes, specific ingredients, allergens, dietary restrictions, prices, or categories like desserts, starters, drinks. " +
            "Filter and select relevant results yourself based on the user question.")
    public synchronized String getAllMenuItems() {
        if (cachedMenuItems == null || isExpired(cachedMenuItemsAt)) {
            LOGGER.debug("Tool getAllMenuItems: loading from DB for establishmentId={}", establishmentId);
            List<MenuItemSearchResult> results = repository.searchMenuItems(
                    new MenuItemSearchQuery(establishmentId));
            cachedMenuItems = formatItems(results);
            cachedMenuItemsAt = Instant.now();
            LOGGER.debug("Tool getAllMenuItems: loaded {} items, {} chars", results.size(), cachedMenuItems.length());
        } else {
            LOGGER.debug("Tool getAllMenuItems: returning cached items ({} chars)", cachedMenuItems.length());
        }
        return cachedMenuItems;
    }
    private static boolean isExpired(@Nullable Instant loadedAt) {
        return loadedAt == null || Instant.now().isAfter(loadedAt.plus(CACHE_TTL));
    }
    // ── Compact text serialization ───────────────────────────────────────────
    private String formatMenus(List<MenuSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (MenuSearchResult r : results) {
            sb.append(localized(r.nameTranslations()))
              .append(" [").append(r.menuCode()).append("]");
            if (r.priceCents() != null) {
                sb.append(" - ").append(formatPrice(r.priceCents(), r.currency()));
            }
            sb.append("\n");
            for (MenuSearchResult.MenuSectionResult s : r.sections()) {
                sb.append("  ").append(localized(s.nameTranslations())).append(": ");
                sb.append(s.items().stream()
                        .map(i -> localized(i.nameTranslations()))
                        .collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
    private String formatItems(List<MenuItemSearchResult> results) {
        // Format: name | price | ingredients | allergens | dietary
        StringBuilder sb = new StringBuilder();
        for (MenuItemSearchResult r : results) {
            sb.append(localized(r.nameTranslations()))
              .append(" | ").append(formatPrice(r.priceCents(), r.currency()))
              .append(" | ").append(localized(r.ingredientNoteTranslations()));
            if (!r.allergenCodes().isEmpty()) {
                sb.append(" | allergens:").append(String.join(",", r.allergenCodes()));
            }
            if (!r.dietaryRestrictionCodes().isEmpty()) {
                sb.append(" | dietary:").append(String.join(",", r.dietaryRestrictionCodes()));
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
    private String localized(Map<String, String> translations) {
        String lang = language.toLowerCase(Locale.ROOT);
        String result = translations.get(lang);
        if (result != null) return result;
        result = translations.get("en");
        if (result != null) return result;
        return translations.values().stream().findFirst().orElse("");
    }
    private static String formatPrice(int cents, String currency) {
        return "%.2f %s".formatted(cents / 100.0, currency);
    }
}