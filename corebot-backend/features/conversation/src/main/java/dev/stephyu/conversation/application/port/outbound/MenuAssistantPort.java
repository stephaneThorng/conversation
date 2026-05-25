package dev.stephyu.conversation.application.port.outbound;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MenuAssistantPort {

    enum Scope {
        MENUS,
        MENU_ITEMS
    }

    /**
     * Answers a natural-language menu or menu-item question for the given establishment.
     * The implementation fetches active menus/items from the repository and lets the LLM
     * generate a natural-language response from that catalogue snapshot.
     *
     * @param sessionId       conversation session id used for chat memory isolation
     * @param userMessage     the original user message
     * @param language        ISO 639-1 language code (e.g. "fr", "en")
     * @param establishmentId UUID of the establishment whose catalogue to search
     * @param scope           whether the request is about menus or menu items
     * @return a natural-language answer
     */
    String answer(String sessionId, String userMessage, String language, String establishmentId, Scope scope);
}

