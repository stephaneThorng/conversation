package dev.stephyu.conversation.application.port.outbound;

import dev.stephyu.conversation.domain.Channel;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConversationAgentPort {

    /**
     * @param channelUserId stable user identifier from the messaging channel (phone, platform ID...)
     * @param channel       the messaging channel used
     * @param establishmentId the restaurant/establishment identifier
     * @param message       the raw user message
     */
    String chat(String channelUserId, Channel channel, String establishmentId, String message);
}
