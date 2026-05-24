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
    public String answer(String sessionId, String userMessage, String language, String establishmentId) {
        LOGGER.debug("MenuAssistant answering: sessionId={}, language={}, establishmentId={}, message={}",
                sessionId, language, establishmentId, userMessage);
        MenuAssistantLlm llm = factory.create(sessionId, establishmentId, language);
        String reply = llm.answer(sessionId, userMessage, language);
        LOGGER.debug("MenuAssistant reply: {}", reply);
        return reply;
    }
}


