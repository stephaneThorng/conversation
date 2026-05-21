package dev.stephyu.conversation.domain.workflow;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDefinition;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Workflow(
        WorkflowType type,
        List<SlotDefinition> slotDefinitions,
        CollectedData collectedData,
        boolean confirmed
) {

    public Workflow {
        Objects.requireNonNull(type, "workflowType must not be null");
        slotDefinitions = List.copyOf(slotDefinitions);
        Objects.requireNonNull(collectedData, "collectedData must not be null");
    }

    public List<SlotDefinition> missingRequiredSlots() {
        return slotDefinitions.stream()
                .filter(SlotDefinition::required)
                .filter(slot -> !collectedData.contains(slot.name()))
                .toList();
    }

    public boolean isReadyForConfirmation() {
        return missingRequiredSlots().isEmpty() && !confirmed;
    }

    public boolean isAwaitingConfirmation() {
        return isReadyForConfirmation();
    }

    public Workflow withCollectedData(CollectedData nextCollectedData) {
        return new Workflow(type, slotDefinitions, nextCollectedData, confirmed);
    }

    public Workflow confirm() {
        return new Workflow(type, slotDefinitions, collectedData, true);
    }
}
