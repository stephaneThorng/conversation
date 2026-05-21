package dev.stephyu.conversation.application.port.outbound;

public interface LlmAssistantPort {

    String chat(String message);
}
