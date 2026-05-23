package dev.stephyu.conversation.application.orchestration;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WorkflowPostProcessResult(
        boolean success,
        String messageKey,
        Map<String, String> arguments
) {

    public WorkflowPostProcessResult {
        Objects.requireNonNull(messageKey, "messageKey must not be null");
        arguments = Map.copyOf(arguments);
    }

    public static WorkflowPostProcessResult success(String messageKey, Map<String, String> arguments) {
        return new WorkflowPostProcessResult(true, messageKey, arguments);
    }

    public static WorkflowPostProcessResult failure(String messageKey, Map<String, String> arguments) {
        return new WorkflowPostProcessResult(false, messageKey, arguments);
    }
}
