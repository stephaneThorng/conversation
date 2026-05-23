package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class IntentHandlerRegistry {

    private final Map<WorkflowType, IntentHandler> handlers;

    public IntentHandlerRegistry(List<IntentHandler> handlers) {
        this.handlers = new EnumMap<>(WorkflowType.class);
        for (IntentHandler handler : handlers) {
            IntentHandler previous = this.handlers.putIfAbsent(handler.workflowType(), handler);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate handler for workflow type " + handler.workflowType());
            }
        }
    }

    public Optional<IntentHandler> get(WorkflowType workflowType) {
        return Optional.ofNullable(handlers.get(workflowType));
    }
}
