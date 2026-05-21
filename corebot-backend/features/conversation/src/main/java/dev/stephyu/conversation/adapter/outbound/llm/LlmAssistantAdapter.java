package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.service.AiServices;
import dev.stephyu.conversation.application.port.outbound.LlmAssistantPort;

public class LlmAssistantAdapter implements LlmAssistantPort {

     private final LlmAssistant assistant;

     private final PersonExtractor personExtractor;

     public LlmAssistantAdapter(LlmAssistant assistant, PersonExtractor personExtractor) {
         this.assistant = assistant;
         this.personExtractor = personExtractor;
     }

    @Override
    public String chat(String message) {


         
        return personExtractor.extractPersonFrom(message).toString();
        //return assistant.chat(message);

    }
}
