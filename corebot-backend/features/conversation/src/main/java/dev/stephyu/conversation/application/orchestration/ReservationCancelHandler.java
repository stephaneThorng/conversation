package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotDefinition;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ReservationCancelHandler implements IntentHandler {

    private static final List<SlotDefinition> SLOT_DEFINITIONS = List.of(
            new SlotDefinition(SlotName.REFERENCE_NUMBER, true, "reservation_cancel.ask_reference_number", List.of()));

    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReservationCancelHandler(ReservationRepositoryPort reservationRepositoryPort) {
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.RESERVATION_CANCEL;
    }

    @Override
    public Workflow newWorkflow() {
        return new Workflow(WorkflowType.RESERVATION_CANCEL, SLOT_DEFINITIONS, CollectedData.empty(), false);
    }

    public Workflow newWorkflowWithReference(String referenceNumber) {
        CollectedData initial = CollectedData.empty()
                .withValue(SlotName.REFERENCE_NUMBER, new SlotDataValue.TextValue(referenceNumber));
        return new Workflow(WorkflowType.RESERVATION_CANCEL, SLOT_DEFINITIONS, initial, false);
    }

    @Override
    public WorkflowPostProcessResult onConfirmed(HandlerInput input, Workflow workflow) {
        String reference = workflow.collectedData()
                .valueAs(SlotName.REFERENCE_NUMBER, SlotDataValue.TextValue.class)
                .orElseThrow()
                .value();
        ReservationRepositoryPort.ReservationResult result = reservationRepositoryPort.cancelReservation(reference);
        if (!result.success()) {
            return WorkflowPostProcessResult.failure("reservation_cancel.not_found", Map.of("reference", reference));
        }
        return WorkflowPostProcessResult.success("reservation_cancel.success", Map.of("reference", reference));
    }

    @Override
    public Map<String, String> confirmationArguments(Workflow workflow) {
        String reference = workflow.collectedData()
                .valueAs(SlotName.REFERENCE_NUMBER, SlotDataValue.TextValue.class)
                .map(SlotDataValue.TextValue::value)
                .orElse("?");
        return Map.of("reference", reference);
    }
}

