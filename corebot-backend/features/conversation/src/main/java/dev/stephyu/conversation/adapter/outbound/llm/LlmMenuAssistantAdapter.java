package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.port.outbound.MenuAssistantPort;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class LlmMenuAssistantAdapter implements MenuAssistantPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmMenuAssistantAdapter.class);

    private final MenuAssistantLlmFactory factory;

    public LlmMenuAssistantAdapter(MenuAssistantLlmFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public String answer(String sessionId, String userMessage, String language, String establishmentId, Scope scope) {
        LOGGER.debug(
                "LLM menu input: sessionId={}, language={}, establishmentId={}, scope={}, message={}",
                sessionId,
                language,
                establishmentId,
                scope,
                quote(singleLine(userMessage, 180)));

        MenuSearchToolProvider tools = factory.tools(sessionId, establishmentId, language);
        String catalogue = scope == Scope.MENUS ? tools.getAllMenus() : tools.getAllMenuItems();
        LOGGER.debug(
                "LLM menu catalogue: sessionId={}, scope={}, chars={}",
                sessionId,
                scope,
                catalogue.length());

        MenuAssistantLlm llm = factory.create(sessionId, establishmentId, language);
        String reply = llm.answer(sessionId, userMessage, language, scope.name(), catalogue);
        LOGGER.debug("LLM menu output: sessionId={}, reply={}", sessionId, quote(singleLine(reply, 240)));
        return reply;
    }

    private static String singleLine(String value, int maxChars) {
        String compact = value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxChars) {
            return compact;
        }
        return compact.substring(0, maxChars) + "...";
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }
}
