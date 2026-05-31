package dev.stephyu.conversation.application.port.outbound;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConversationAgentPort {

    /**
     * Sends a user message to the conversation agent and returns the assistant reply.
     *
     * @param sessionId       unique session identifier (used as memory key)
     * @param establishmentId the restaurant/establishment identifier
     * @param message         the raw user message
     * @return the assistant reply as plain text
     */
    String chat(String sessionId, String establishmentId, String message);
}

