package dev.stephyu.conversation.adapter.outbound.llm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.service.SystemMessage;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

final class ConversationAgentLlmPromptTest {

    @Test
    void shouldReferenceOpeningHoursAndCapacityToolsInSystemPrompt() throws Exception {
        Method chatMethod = ConversationAgentLlm.class.getMethod(
                "chat",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class);

        SystemMessage systemMessage = chatMethod.getAnnotation(SystemMessage.class);
        String prompt = String.join("\n", systemMessage.value());
        assertTrue(prompt.contains("plain text only"));
        assertTrue(prompt.contains("OpeningHoursTools"));
        assertTrue(prompt.contains("getOpeningHours"));
        assertTrue(prompt.contains("checkReservationCapacity"));
        assertTrue(prompt.contains("opening hours"));
        assertTrue(prompt.contains("closures"));
    }
}
