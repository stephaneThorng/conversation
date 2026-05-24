package dev.stephyu.conversation.application.orchestration;

import java.util.Map;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface IntentHandler {

    WorkflowType workflowType();

    Workflow newWorkflow();

    Map<String, String> confirmationArguments(Workflow workflow);

    WorkflowPostProcessResult onConfirmed(HandlerInput input, Workflow workflow);

    /**
     * Returns true when all required slots are filled and the workflow should execute
     * immediately without showing a confirmation summary to the user.
     * Defaults to false (show confirmation summary first).
     */
    default boolean skipConfirmation() {
        return false;
    }
}
