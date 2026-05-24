package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class TokenUsageLoggingListener implements ChatModelListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenUsageLoggingListener.class);

    private final String modelName;

    public TokenUsageLoggingListener(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // nothing to log before the response
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        TokenUsage usage = responseContext.chatResponse().metadata().tokenUsage();
        if (usage != null) {
            LOGGER.debug("LLM [{}] token usage: input={} output={} total={}",
                    modelName,
                    usage.inputTokenCount(),
                    usage.outputTokenCount(),
                    usage.totalTokenCount());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        LOGGER.debug("LLM [{}] request failed: {}", modelName, errorContext.error().getMessage());
    }
}

