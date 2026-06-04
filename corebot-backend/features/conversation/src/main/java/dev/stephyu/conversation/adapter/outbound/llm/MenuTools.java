package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LLM-callable tools for menu queries.
 * establishmentId and language are passed explicitly by the LLM so a single instance
 * serves all establishments. Results are cached per establishment/language pair (TTL 10 min).
 * <p>
 * Compact pipe-separated text format to minimize token consumption:
 * items  → name | price | ingredients | allergens | dietary
 * menus  → name [code] price\n  Section: item1, item2
 */
@NullMarked
public final class MenuTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuTools.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final SearchMenuRepositoryPort repository;

    // Cache keyed by "establishmentId|language"
    private final ConcurrentHashMap<String, CachedEntry> menusCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedEntry> itemsCache = new ConcurrentHashMap<>();

    public MenuTools(SearchMenuRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Tool("Returns the active menus for the given establishment and language. " +
            "Use when the user asks about menus, set menus, meal deals, or the overall menu structure.")
    public String getAllMenus(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("User language code, e.g. fr, en, de") String language) {
        String key = cacheKey(establishmentId, language);
        CachedEntry entry = menusCache.compute(key, (k, existing) -> {
            if (existing != null && !existing.isExpired()) {
                return existing;
            }
            LOGGER.debug("Tool getAllMenus: loading from DB for establishmentId={}, language={}", establishmentId, language);
            UUID uuid = UUID.fromString(establishmentId);
            List<MenuSearchResult> results = repository.searchMenus(new MenuSearchQuery(uuid));
            String formatted = formatMenus(results, normalize(language));
            LOGGER.debug("Tool getAllMenus: loaded {} menus, {} chars", results.size(), formatted.length());
            return new CachedEntry(formatted);
        });
        return entry.value();
    }

    @Tool("Returns all active dishes/items for the given establishment and language, " +
            "including name, price, ingredients, allergens and dietary labels. " +
            "Use when the user asks about dishes, ingredients, allergens, dietary restrictions, prices, or categories.")
    public String getAllMenuItems(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("User language code, e.g. fr, en, de") String language) {
        String key = cacheKey(establishmentId, language);
        CachedEntry entry = itemsCache.compute(key, (k, existing) -> {
            if (existing != null && !existing.isExpired()) {
                return existing;
            }
            LOGGER.debug("Tool getAllMenuItems: loading from DB for establishmentId={}, language={}", establishmentId, language);
            UUID uuid = UUID.fromString(establishmentId);
            List<MenuItemSearchResult> results = repository.searchMenuItems(new MenuItemSearchQuery(uuid));
            String formatted = formatItems(results, normalize(language));
            LOGGER.debug("Tool getAllMenuItems: loaded {} items, {} chars", results.size(), formatted.length());
            return new CachedEntry(formatted);
        });
        return entry.value();
    }

    // ── Formatting ───────────────────────────────────────────────────────────

    private static String formatMenus(List<MenuSearchResult> results, String language) {
        StringBuilder sb = new StringBuilder();
        for (MenuSearchResult r : results) {
            sb.append(localized(r.nameTranslations(), language))
                    .append(" [").append(r.menuCode()).append("]");
            if (r.priceCents() != null) {
                sb.append(" - ").append(formatPrice(r.priceCents(), r.currency()));
            }
            sb.append("\n");
            for (MenuSearchResult.MenuSectionResult s : r.sections()) {
                sb.append("  ").append(localized(s.nameTranslations(), language)).append(": ");
                sb.append(s.items().stream()
                        .map(i -> localized(i.nameTranslations(), language))
                        .collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatItems(List<MenuItemSearchResult> results, String language) {
        StringBuilder sb = new StringBuilder();
        for (MenuItemSearchResult r : results) {
            sb.append(localized(r.nameTranslations(), language))
                    .append(" | ").append(formatPrice(r.priceCents(), r.currency()))
                    .append(" | ").append(localized(r.ingredientNoteTranslations(), language));
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

    private static String localized(Map<String, String> translations, String language) {
        String result = translations.get(language);
        if (result != null) return result;
        result = translations.get("en");
        if (result != null) return result;
        return translations.values().stream().findFirst().orElse("");
    }

    private static String formatPrice(int cents, String currency) {
        return "%.2f %s".formatted(cents / 100.0, currency);
    }

    private static String normalize(String language) {
        return (language == null || language.isBlank()) ? "en" : language.toLowerCase(Locale.ROOT);
    }

    private static String cacheKey(String establishmentId, String language) {
        return establishmentId + "|" + normalize(language);
    }

    // ── Cache entry ──────────────────────────────────────────────────────────

    private static final class CachedEntry {
        private final String value;
        private final Instant loadedAt;

        CachedEntry(String value) {
            this.value = value;
            this.loadedAt = Instant.now();
        }

        String value() {
            return value;
        }

        boolean isExpired() {
            return Instant.now().isAfter(loadedAt.plus(CACHE_TTL));
        }
    }
}

