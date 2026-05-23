package dev.stephyu.config.dependency;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.adapter.inbound.web.UserController;
import dev.stephyu.conversation.adapter.outbound.llm.ConversationAnalyzerLlm;
import dev.stephyu.conversation.adapter.outbound.llm.LlmAssistant;
import dev.stephyu.conversation.adapter.outbound.llm.LlmAssistantAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.LlmConversationAnalyzerAdapter;
import dev.stephyu.conversation.adapter.outbound.llm.PersonExtractor;
import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.adapter.outbound.persistence.FakeReservationAdapter;
import dev.stephyu.conversation.adapter.outbound.persistence.InMemoryConversationStateRepository;
import dev.stephyu.conversation.adapter.outbound.reply.PropertiesConversationReplyCatalog;
import dev.stephyu.conversation.adapter.outbound.reply.StaticEstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.HandleConversationService;
import dev.stephyu.conversation.application.orchestration.ConversationOrchestrator;
import dev.stephyu.conversation.application.orchestration.IntentHandlerRegistry;
import dev.stephyu.conversation.application.orchestration.ReservationCreateHandler;
import dev.stephyu.conversation.application.orchestration.SlotValueNormalizer;
import dev.stephyu.conversation.application.orchestration.WorkflowProcessor;
import dev.stephyu.conversation.application.orchestration.WorkflowReplyResolver;
import dev.stephyu.conversation.application.port.outbound.ConversationAnalyzerPort;
import dev.stephyu.conversation.application.port.outbound.ConversationStateRepositoryPort;
import dev.stephyu.conversation.application.port.outbound.ReservationPort;
import dev.stephyu.conversation.application.reply.ConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.EstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import dev.stephyu.config.web.HttpEndpoint;
import java.util.List;
import org.jspecify.annotations.NullMarked;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
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

        OllamaChatModel analyzerModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("gemma3n:e4b")
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .build();

        LlmAssistant assistant = AiServices.builder(LlmAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, model);
        new LlmAssistantAdapter(assistant, personExtractor);
        ConversationStateRepositoryPort conversationStateRepository = new InMemoryConversationStateRepository();
        ConversationAnalyzerLlm conversationAnalyzerLlm = AiServices.builder(ConversationAnalyzerLlm.class)
                .chatModel(analyzerModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
        ConversationAnalyzerPort conversationAnalyzerPort = new LlmConversationAnalyzerAdapter(conversationAnalyzerLlm);
        ConversationReplyCatalog replyCatalog = new PropertiesConversationReplyCatalog();
        EstablishmentResponseStyleResolver responseStyleResolver = new StaticEstablishmentResponseStyleResolver();
        SlotValueNormalizer slotValueNormalizer = new RecognizersTextSlotValueNormalizer();
        ReservationPort reservationPort = new FakeReservationAdapter();
        WorkflowProcessor workflowProcessor = new WorkflowProcessor(slotValueNormalizer);
        WorkflowReplyResolver workflowReplyResolver = new WorkflowReplyResolver(replyCatalog);
        ReservationCreateHandler reservationCreateHandler = new ReservationCreateHandler(reservationPort);
        IntentHandlerRegistry intentHandlerRegistry = new IntentHandlerRegistry(List.of(reservationCreateHandler));
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
