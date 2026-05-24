package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MenuAssistantLlmFactory {

    private final ChatModel chatModel;
    private final SearchMenuRepositoryPort searchMenuRepositoryPort;

    // One MenuSearchToolProvider per establishment — catalogue is read-only so it is safe to share
    // across sessions. A second session for the same establishment reuses the already-loaded data.
    private final Map<String, MenuSearchToolProvider> toolCache = new ConcurrentHashMap<>();

    public MenuAssistantLlmFactory(ChatModel chatModel, SearchMenuRepositoryPort searchMenuRepositoryPort) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.searchMenuRepositoryPort = Objects.requireNonNull(searchMenuRepositoryPort, "searchMenuRepositoryPort must not be null");
    }

    public MenuAssistantLlm create(String sessionId, String establishmentId, String language) {
        MenuSearchToolProvider tools = toolCache.computeIfAbsent(establishmentId, ignored ->
                new MenuSearchToolProvider(
                        searchMenuRepositoryPort,
                        UUID.fromString(establishmentId),
                        language));
        return AiServices.builder(MenuAssistantLlm.class)
                .chatModel(chatModel)
                .tools(tools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(6))
                .build();
    }
}



