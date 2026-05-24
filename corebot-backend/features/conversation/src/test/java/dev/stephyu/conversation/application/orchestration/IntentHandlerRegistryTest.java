package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class IntentHandlerRegistryTest {

    @Test
    void findsHandlerByWorkflowType() {
        IntentHandlerRegistry registry = new IntentHandlerRegistry(List.of(new StubHandler()));

        assertEquals(WorkflowType.RESERVATION_CREATE, registry.get(WorkflowType.RESERVATION_CREATE).orElseThrow().workflowType());
    }

    @Test
    void rejectsDuplicateHandlers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IntentHandlerRegistry(List.of(new StubHandler(), new StubHandler())));
    }

    private static final class StubHandler implements IntentHandler {
        @Override
        public WorkflowType workflowType() {
            return WorkflowType.RESERVATION_CREATE;
        }

        @Override
        public Workflow newWorkflow() {
            return new Workflow(WorkflowType.RESERVATION_CREATE, List.of(), CollectedData.empty(), false);
        }

        @Override
        public Map<String, String> confirmationArguments(Workflow workflow) {
            return Map.of();
        }

        @Override
        public WorkflowPostProcessResult onConfirmed(@NonNull HandlerInput input, Workflow workflow) {
            return WorkflowPostProcessResult.success("workflow.success", Map.of());
        }
    }
}
