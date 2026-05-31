package dev.stephyu.config.dependency;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.config.app.LlmModelConfig;
import dev.stephyu.conversation.adapter.inbound.web.UserController;
import dev.stephyu.conversation.adapter.outbound.llm.ConversationAgentAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.ConversationAgentLlm;
import dev.stephyu.conversation.adapter.outbound.llm.MenuTools;
import dev.stephyu.conversation.adapter.outbound.llm.OpeningHoursTools;
import dev.stephyu.conversation.adapter.outbound.llm.ReservationTools;
import dev.stephyu.conversation.adapter.outbound.llm.TokenUsageLoggingListener;
import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.adapter.outbound.persistence.postgres.PostgresRestaurantAvailabilityRepository;
import dev.stephyu.conversation.adapter.outbound.persistence.postgres.PostgresReservationRepository;
import dev.stephyu.conversation.application.HandleConversationService;
import dev.stephyu.conversation.application.port.outbound.ConversationAgentPort;
import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.RestaurantAvailabilityRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.config.web.HttpEndpoint;
import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConversationModule {

    public List<HttpEndpoint> httpEndpoints(SearchMenuRepositoryPort searchMenuRepositoryPort, DSLContext dslContext) {
        Objects.requireNonNull(searchMenuRepositoryPort, "searchMenuRepositoryPort must not be null");
        Objects.requireNonNull(dslContext, "dslContext must not be null");
        LlmModelConfig config = LlmModelConfig.fromEnvironment();

        // ── Single DeepSeek V4 Flash model via OpenAI-compatible API ──
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .modelName(config.modelName())
                .temperature(0.3)
                .listeners(List.of(new TokenUsageLoggingListener("agent")))
                .build();

        // ── Tools ──
        ReservationRepositoryPort reservationRepository = new PostgresReservationRepository(dslContext);
        RestaurantAvailabilityRepositoryPort availabilityRepository =
                new PostgresRestaurantAvailabilityRepository(dslContext);
        ReservationTools reservationTools = new ReservationTools(reservationRepository);
        OpeningHoursTools openingHoursTools = new OpeningHoursTools(availabilityRepository);
        MenuTools menuTools = new MenuTools(searchMenuRepositoryPort);

        // ── Single agent LLM with memory per session ──
        ConversationAgentLlm agentLlm = AiServices.builder(ConversationAgentLlm.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .tools(reservationTools, openingHoursTools, menuTools)
                .build();

        ConversationAgentPort conversationAgentPort = new ConversationAgentAdapter(agentLlm);

        // ── Application layer ──
        ConversationStateRepositoryPort conversationStateRepository = new InMemoryConversationStateRepository();
        HandleConversationUseCase handleConversationUseCase = new HandleConversationService(
                conversationStateRepository,
                conversationAgentPort);

        UserController userController = new UserController(handleConversationUseCase);
        return List.of(userController::register);
    }
}
