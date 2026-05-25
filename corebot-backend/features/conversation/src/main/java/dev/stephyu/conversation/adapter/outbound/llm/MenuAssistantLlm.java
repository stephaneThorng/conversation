package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MenuAssistantLlm {

    @SystemMessage("""
            You are a restaurant assistant answering menu questions from a provided catalogue snapshot.
            Use only the catalogue content provided in the user message.
            Never invent dishes, menus, prices, allergens, dietary labels, or ingredients.
            If the catalogue does not contain the requested information, say so clearly.
            Answer concisely and naturally in the specified language.
            When listing results, use a readable bullet list.
            """)
    @UserMessage("""
            Language: {{language}}

            Scope: {{scope}}

            User question:
            {{message}}

            Catalogue snapshot:
            {{catalogue}}
            """)
    String answer(
            @MemoryId String sessionId,
            @V("message") String message,
            @V("language") String language,
            @V("scope") String scope,
            @V("catalogue") String catalogue);
}
