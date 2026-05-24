package dev.stephyu.conversation.application.orchestration;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record WorkflowPostProcessResult(
        boolean success,
        Map<String, String> arguments,
        @Nullable String reservationReference,
        @Nullable String directReply
) {

    public WorkflowPostProcessResult {
        arguments = Map.copyOf(arguments);
    }

    public static WorkflowPostProcessResult success(Map<String, String> arguments) {
        return new WorkflowPostProcessResult(true, arguments, null, null);
    }

    public static WorkflowPostProcessResult success(Map<String, String> arguments, String reservationReference) {
        return new WorkflowPostProcessResult(true, arguments, reservationReference, null);
    }

    public static WorkflowPostProcessResult failure(Map<String, String> arguments) {
        return new WorkflowPostProcessResult(false, arguments, null, null);
    }

    public static WorkflowPostProcessResult directReply(String reply) {
        return new WorkflowPostProcessResult(true, Map.of(), null, reply);
    }
}
