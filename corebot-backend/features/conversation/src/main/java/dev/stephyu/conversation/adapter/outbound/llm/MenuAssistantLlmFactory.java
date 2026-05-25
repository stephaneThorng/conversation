package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class MenuAssistantLlmFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuAssistantLlmFactory.class);

    private final ChatModel chatModel;
    private final SearchMenuRepositoryPort searchMenuRepositoryPort;

    // One provider per establishment/language pair so localized catalogue strings stay coherent.
    private final Map<String, MenuSearchToolProvider> toolCache = new ConcurrentHashMap<>();

    public MenuAssistantLlmFactory(ChatModel chatModel, SearchMenuRepositoryPort searchMenuRepositoryPort) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.searchMenuRepositoryPort = Objects.requireNonNull(searchMenuRepositoryPort, "searchMenuRepositoryPort must not be null");
    }

    public MenuAssistantLlm create(String sessionId, String establishmentId, String language) {
        return AiServices.builder(MenuAssistantLlm.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(6))
                .build();
    }

    public MenuSearchToolProvider tools(String sessionId, String establishmentId, String language) {
        String cacheKey = establishmentId + "|" + language.toLowerCase(Locale.ROOT);
        boolean cacheHit = toolCache.containsKey(cacheKey);
        MenuSearchToolProvider tools = toolCache.computeIfAbsent(cacheKey, ignored ->
                new MenuSearchToolProvider(
                        searchMenuRepositoryPort,
                        UUID.fromString(establishmentId),
                        language));
        LOGGER.debug(
                "LLM menu tools: sessionId={}, establishmentId={}, language={}, cacheHit={}",
                sessionId,
                establishmentId,
                language,
                cacheHit);
        return tools;
    }
}
