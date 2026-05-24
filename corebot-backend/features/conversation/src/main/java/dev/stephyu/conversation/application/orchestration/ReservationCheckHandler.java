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
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ReservationCheckHandler implements IntentHandler {

    private static final List<SlotDefinition> SLOT_DEFINITIONS = List.of(
            new SlotDefinition(SlotName.REFERENCE_NUMBER, true, List.of()));

    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReservationCheckHandler(ReservationRepositoryPort reservationRepositoryPort) {
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.RESERVATION_CHECK;
    }

    @Override
    public Workflow newWorkflow() {
        CollectedData initial = CollectedData.empty();
        // Pre-fill reference_number from session state if available
        return new Workflow(WorkflowType.RESERVATION_CHECK, SLOT_DEFINITIONS, initial, false);
    }

    public Workflow newWorkflowWithReference(String referenceNumber) {
        CollectedData initial = CollectedData.empty()
                .withValue(SlotName.REFERENCE_NUMBER, new SlotDataValue.TextValue(referenceNumber));
        return new Workflow(WorkflowType.RESERVATION_CHECK, SLOT_DEFINITIONS, initial, false);
    }

    @Override
    public boolean skipConfirmation() {
        return true;
    }

    @Override
    public WorkflowPostProcessResult onConfirmed(HandlerInput input, Workflow workflow) {
        String reference = workflow.collectedData()
                .valueAs(SlotName.REFERENCE_NUMBER, SlotDataValue.TextValue.class)
                .orElseThrow()
                .value();
        Optional<ReservationRepositoryPort.ReservationSummary> summary = reservationRepositoryPort.findReservation(reference);
        if (summary.isEmpty()) {
            return WorkflowPostProcessResult.failure(Map.of(
                    "reference", reference,
                    "action_description", "The user wanted to check reservation " + reference + " but no reservation was found with that reference."));
        }
        ReservationRepositoryPort.ReservationSummary res = summary.get();
        return WorkflowPostProcessResult.success(Map.of(
                "reference", res.referenceNumber(),
                "reservation_name", res.reservationName(),
                "date", res.date().toString(),
                "time", res.time().toString(),
                "people_count", Integer.toString(res.peopleCount()),
                "action_description", "The user checked their reservation " + reference + ". Here are the details found."));
    }

    @Override
    public Map<String, String> confirmationArguments(Workflow workflow) {
        return Map.of();
    }
}

