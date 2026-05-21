package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.service.UserMessage;

public interface PersonExtractor {

    @UserMessage("""
            Extract information about a person from {{it}}.
            Return only valid JSON.
            Use exactly these top-level keys: firstname, lastname, intent, address.
            Do not use misspelled keys such as firsname.
            If a value is unknown, use null.
            """)
    LlmResult extractPersonFrom(String text);
}
