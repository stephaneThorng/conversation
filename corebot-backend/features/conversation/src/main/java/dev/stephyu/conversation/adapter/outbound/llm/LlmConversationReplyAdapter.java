package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.port.outbound.ConversationReplyPort;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class LlmConversationReplyAdapter implements ConversationReplyPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmConversationReplyAdapter.class);

    private final ConversationReplyLlm llm;

    public LlmConversationReplyAdapter(ConversationReplyLlm llm) {
        this.llm = Objects.requireNonNull(llm, "llm must not be null");
    }

    @Override
    public String reply(String sessionId, String language, ReplyContext context, String userMessage) {
        String facts = formatFacts(context.facts());
        LOGGER.debug("ConversationReply generating: sessionId={}, language={}, intent={}", sessionId, language, context.replyIntent());
        String reply = llm.generate(language, context.replyIntent().name(), facts, userMessage);
        LOGGER.debug("ConversationReply generated: {}", reply);
        return reply;
    }

    private static String formatFacts(Map<String, String> facts) {
        if (facts.isEmpty()) {
            return "none";
        }
        return facts.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }
}

