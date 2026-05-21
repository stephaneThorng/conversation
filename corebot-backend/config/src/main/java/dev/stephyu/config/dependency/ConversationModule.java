package dev.stephyu.config.dependency;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.adapter.inbound.web.UserController;
import dev.stephyu.conversation.adapter.outbound.llm.LlmAssistant;
import dev.stephyu.conversation.adapter.outbound.llm.LlmAssistantAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.PersonExtractor;
import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.application.HandleConversationService;
import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.LlmAssistantPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.config.web.HttpEndpoint;
import java.util.List;
import org.jspecify.annotations.NullMarked;

import static dev.langchain4j.model.chat.request.ResponseFormat.JSON;

@NullMarked
public final class ConversationModule {

    public List<HttpEndpoint> httpEndpoints() {

        // AI Dependencies
        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("gemma3n:e4b")
                .responseFormat(JSON)
                .build();

        LlmAssistant assistant = AiServices.builder(LlmAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, model);
        LlmAssistantPort llmAssistantPort = new LlmAssistantAdapter(assistant, personExtractor);
        ConversationStateRepositoryPort conversationStateRepository = new InMemoryConversationStateRepository();


        HandleConversationUseCase handleConversationUseCase = new HandleConversationService(
                llmAssistantPort,
                conversationStateRepository);
        UserController userController = new UserController(handleConversationUseCase);


        return List.of(userController::register);
    }
}
