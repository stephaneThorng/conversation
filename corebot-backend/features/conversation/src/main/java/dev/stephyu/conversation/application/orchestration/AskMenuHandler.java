package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.MenuAssistantPort;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDefinition;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AskMenuHandler implements IntentHandler {

    private static final List<SlotDefinition> SLOT_DEFINITIONS = List.of();

    private final MenuAssistantPort menuAssistantPort;

    public AskMenuHandler(MenuAssistantPort menuAssistantPort) {
        this.menuAssistantPort = Objects.requireNonNull(menuAssistantPort, "menuAssistantPort must not be null");
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ASK_MENU;
    }

    @Override
    public Workflow newWorkflow() {
        return new Workflow(WorkflowType.ASK_MENU, SLOT_DEFINITIONS, CollectedData.empty(), false);
    }

    @Override
    public boolean skipConfirmation() {
        return true;
    }

    @Override
    public WorkflowPostProcessResult onConfirmed(HandlerInput input, Workflow workflow) {
        String establishmentId = input.session().state().establishmentId().value();
        String reply = menuAssistantPort.answer(
                input.session().sessionId().value(),
                input.message(),
                input.language(),
                establishmentId,
                MenuAssistantPort.Scope.MENUS);
        return WorkflowPostProcessResult.directReply(reply);
    }

    @Override
    public Map<String, String> confirmationArguments(Workflow workflow) {
        return Map.of();
    }
}
