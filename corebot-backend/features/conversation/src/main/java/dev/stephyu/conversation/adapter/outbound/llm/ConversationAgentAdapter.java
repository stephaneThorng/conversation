package dev.stephyu.conversation.adapter.outbound.llm;

import dev.stephyu.conversation.application.port.outbound.ConversationAgentPort;
import dev.stephyu.conversation.domain.Channel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.ZoneId;
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
    public String chat(String channelUserId, Channel channel, String establishmentId, String message) {
        // Memory key is stable across sessions: same user on same establishment = same conversation history
        String memoryKey = channelUserId + "|" + establishmentId;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String currentDate = today.format(DATE_FORMATTER);
        String currentDayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        LOGGER.debug("Agent input: channelUserId={}, channel={}, establishmentId={}, date={} ({}), message={}",
                channelUserId, channel, establishmentId, currentDate, currentDayOfWeek, message);

        String reply = agentLlm.chat(memoryKey, establishmentId, channel.name(), channelUserId, currentDate, currentDayOfWeek, message);

        LOGGER.debug("Agent output: channelUserId={}, reply={}", channelUserId, singleLine(reply, 240));
        return reply;
    }

    private static String singleLine(String value, int maxChars) {
        String compact = value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return compact.length() <= maxChars ? compact : compact.substring(0, maxChars) + "...";
    }
}
