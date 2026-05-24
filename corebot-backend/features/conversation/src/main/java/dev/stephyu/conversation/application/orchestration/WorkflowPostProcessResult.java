package dev.stephyu.conversation.application.orchestration;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record WorkflowPostProcessResult(
        boolean success,
        String messageKey,
        Map<String, String> arguments,
        @Nullable String reservationReference
) {

    public WorkflowPostProcessResult {
        Objects.requireNonNull(messageKey, "messageKey must not be null");
        arguments = Map.copyOf(arguments);
    }

    public static WorkflowPostProcessResult success(String messageKey, Map<String, String> arguments) {
        return new WorkflowPostProcessResult(true, messageKey, arguments, null);
    }

    public static WorkflowPostProcessResult success(String messageKey, Map<String, String> arguments, String reservationReference) {
        return new WorkflowPostProcessResult(true, messageKey, arguments, reservationReference);
    }

    public static WorkflowPostProcessResult failure(String messageKey, Map<String, String> arguments) {
        return new WorkflowPostProcessResult(false, messageKey, arguments, null);
    }
}
