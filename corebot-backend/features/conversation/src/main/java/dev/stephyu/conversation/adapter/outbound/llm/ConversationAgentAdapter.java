package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.port.outbound.ConversationAgentPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class ConversationAgentAdapter implements ConversationAgentPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationAgentAdapter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ConversationAgentLlm agentLlm;

    public ConversationAgentAdapter(ConversationAgentLlm agentLlm) {
        this.agentLlm = Objects.requireNonNull(agentLlm, "agentLlm must not be null");
    }

    @Override
    public String chat(String sessionId, String establishmentId, String message) {
        LocalDate today = LocalDate.now();
        String currentDate = today.format(DATE_FORMATTER);
        String currentDayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        LOGGER.debug("Agent input: sessionId={}, establishmentId={}, date={} ({}), message={}",
                sessionId, establishmentId, currentDate, currentDayOfWeek, message);

        String reply = agentLlm.chat(sessionId, establishmentId, sessionId, currentDate, currentDayOfWeek, message);

        LOGGER.debug("Agent output: sessionId={}, reply={}", sessionId, singleLine(reply, 240));
        return reply;
    }

    private static String singleLine(String value, int maxChars) {
        String compact = value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return compact.length() <= maxChars ? compact : compact.substring(0, maxChars) + "...";
    }
}
