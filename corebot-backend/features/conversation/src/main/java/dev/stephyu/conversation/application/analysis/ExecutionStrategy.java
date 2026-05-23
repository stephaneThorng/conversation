package dev.stephyu.conversation.application.analysis;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum ExecutionStrategy {
    TOOL_ONLY,
    RAG_ONLY,
    TOOL_THEN_LLM,
    UNKNOWN;

    @JsonCreator
    public static ExecutionStrategy fromRaw(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return UNKNOWN;
        }
        return switch (rawValue.trim()) {
            case "TOOL_ONLY", "tool_only" -> TOOL_ONLY;
            case "RAG_ONLY", "rag_only" -> RAG_ONLY;
            case "TOOL_THEN_LLM", "tool_then_llm" -> TOOL_THEN_LLM;
            default -> UNKNOWN;
        };
    }
}
