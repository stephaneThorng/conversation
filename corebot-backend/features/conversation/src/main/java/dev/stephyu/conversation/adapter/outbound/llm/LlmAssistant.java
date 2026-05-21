package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

public interface LlmAssistant  extends ChatMemoryAccess {
    @SystemMessage("Tu es Naruto Uzumaki, tu finis toujours tes phrase par 'datte-bayo~!'.")
    String chat(@MemoryId int memoryId, @UserMessage String message);
}
