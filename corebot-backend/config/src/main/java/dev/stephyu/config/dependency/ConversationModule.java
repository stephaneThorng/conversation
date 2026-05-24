package dev.stephyu.config.dependency;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.adapter.inbound.web.UserController;
import dev.stephyu.conversation.adapter.outbound.llm.ConversationAnalyzerLlm;
import dev.stephyu.conversation.adapter.outbound.llm.LlmConversationAnalyzerAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.LlmMenuAssistantAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.MenuAssistantLlmFactory;
import dev.stephyu.conversation.adapter.outbound.llm.TokenUsageLoggingListener;
import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.adapter.outbound.persistence.FakeReservationAdapter;
import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.adapter.outbound.reply.PropertiesConversationReplyCatalog;
import dev.stephyu.conversation.adapter.outbound.reply.StaticEstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.HandleConversationService;
import dev.stephyu.conversation.application.orchestration.ConversationOrchestrator;
import dev.stephyu.conversation.application.orchestration.IntentHandlerRegistry;
import dev.stephyu.conversation.application.orchestration.AskMenuHandler;
import dev.stephyu.conversation.application.orchestration.AskMenuItemHandler;
import dev.stephyu.conversation.application.orchestration.ReservationCreateHandler;
import dev.stephyu.conversation.application.orchestration.ReservationCheckHandler;
import dev.stephyu.conversation.application.orchestration.ReservationCancelHandler;
import dev.stephyu.conversation.application.orchestration.SlotValueNormalizer;
import dev.stephyu.conversation.application.orchestration.WorkflowProcessor;
import dev.stephyu.conversation.application.orchestration.WorkflowReplyResolver;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.MenuAssistantPort;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import dev.stephyu.conversation.application.reply.ConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.EstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.config.web.HttpEndpoint;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

@NullMarked
public final class ConversationModule {

    public List<HttpEndpoint> httpEndpoints(SearchMenuRepositoryPort searchMenuRepositoryPort) {
        Objects.requireNonNull(searchMenuRepositoryPort, "searchMenuRepositoryPort must not be null");

        // ── Analyzer model: structured JSON extraction (requires JSON schema support) ──
        OllamaChatModel analyzerModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3.5:4b")
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .temperature(0.0)
                .topP(0.1)
                .think(false)
                .listeners(List.of(new TokenUsageLoggingListener("analyzer")))
                .build();

        // ── Assistant model: tool calling + natural-language generation ──
        ChatModel assistantModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3.5:4b")
                .temperature(0.3)
                .think(false)
                .listeners(List.of(new TokenUsageLoggingListener("menu-assistant")))
                .build();

        ConversationStateRepositoryPort conversationStateRepository = new InMemoryConversationStateRepository();
        ConversationAnalyzerLlm conversationAnalyzerLlm = AiServices.builder(ConversationAnalyzerLlm.class)
                .chatModel(analyzerModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
        ConversationAnalyzerPort conversationAnalyzerPort = new LlmConversationAnalyzerAdapter(conversationAnalyzerLlm);

        MenuAssistantLlmFactory menuAssistantLlmFactory = new MenuAssistantLlmFactory(assistantModel, searchMenuRepositoryPort);
        MenuAssistantPort menuAssistantPort = new LlmMenuAssistantAdapter(menuAssistantLlmFactory);

        ConversationReplyCatalog replyCatalog = new PropertiesConversationReplyCatalog();
        EstablishmentResponseStyleResolver responseStyleResolver = new StaticEstablishmentResponseStyleResolver();
        SlotValueNormalizer slotValueNormalizer = new RecognizersTextSlotValueNormalizer();
        ReservationRepositoryPort reservationRepositoryPort = new FakeReservationAdapter();
        WorkflowProcessor workflowProcessor = new WorkflowProcessor(slotValueNormalizer);
        WorkflowReplyResolver workflowReplyResolver = new WorkflowReplyResolver(replyCatalog);
        ReservationCreateHandler reservationCreateHandler = new ReservationCreateHandler(reservationRepositoryPort);
        ReservationCheckHandler reservationCheckHandler = new ReservationCheckHandler(reservationRepositoryPort);
        ReservationCancelHandler reservationCancelHandler = new ReservationCancelHandler(reservationRepositoryPort);
        AskMenuHandler askMenuHandler = new AskMenuHandler(menuAssistantPort);
        AskMenuItemHandler askMenuItemHandler = new AskMenuItemHandler(menuAssistantPort);
        IntentHandlerRegistry intentHandlerRegistry = new IntentHandlerRegistry(List.of(
                reservationCreateHandler,
                reservationCheckHandler,
                reservationCancelHandler,
                askMenuHandler,
                askMenuItemHandler));
        ConversationOrchestrator conversationOrchestrator = new ConversationOrchestrator(
                conversationAnalyzerPort,
                intentHandlerRegistry,
                replyCatalog,
                responseStyleResolver,
                workflowProcessor,
                workflowReplyResolver);

        HandleConversationUseCase handleConversationUseCase = new HandleConversationService(
                conversationStateRepository,
                conversationOrchestrator);
        UserController userController = new UserController(handleConversationUseCase);

        return List.of(userController::register);
    }
}
