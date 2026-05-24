package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MenuAssistantLlm {

    @SystemMessage("You are a friendly restaurant assistant with access to the establishment menu catalogue. " +
            "Use the available tools to search for menus and dishes before answering. " +
            "Rules: always call a tool to retrieve data, never invent dishes or prices. " +
            "Answer concisely and naturally in the language of the user message. " +
            "If no results match, say so politely. " +
            "When listing results use a clear readable format with bullet points. " +
            "Highlight allergens, dietary labels and price when relevant to the question.")
    @UserMessage("Language: {{language}}\n\nUser: {{message}}")
    String answer(
            @MemoryId String sessionId,
            @V("message") String message,
            @V("language") String language);
}
