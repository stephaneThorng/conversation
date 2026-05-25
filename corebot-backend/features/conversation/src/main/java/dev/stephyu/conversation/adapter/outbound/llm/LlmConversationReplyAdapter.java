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
        LOGGER.debug(
                "LLM reply input: sessionId={}, language={}, intent={}, facts={}, userMessage={}",
                sessionId,
                language,
                context.replyIntent(),
                quote(singleLine(facts, 240)),
                quote(singleLine(userMessage, 180)));
        String reply = llm.generate(language, context.replyIntent().name(), facts, userMessage);
        LOGGER.debug("LLM reply output: sessionId={}, reply={}", sessionId, quote(singleLine(reply, 240)));
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

