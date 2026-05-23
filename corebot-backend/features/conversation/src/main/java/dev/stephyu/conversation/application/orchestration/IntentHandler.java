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
}
