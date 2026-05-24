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
public final class ReservationCreateHandler implements IntentHandler {

    private static final List<SlotDefinition> SLOT_DEFINITIONS = List.of(
            new SlotDefinition(SlotName.RESERVATION_NAME, true, List.of()),
            new SlotDefinition(SlotName.DATE, true, List.of()),
            new SlotDefinition(SlotName.TIME, true, List.of()),
            new SlotDefinition(SlotName.PEOPLE_COUNT, true, List.of()));

    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReservationCreateHandler(ReservationRepositoryPort reservationRepositoryPort) {
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.RESERVATION_CREATE;
    }

    @Override
    public Workflow newWorkflow() {
        return new Workflow(WorkflowType.RESERVATION_CREATE, SLOT_DEFINITIONS, CollectedData.empty(), false);
    }

    @Override
    public WorkflowPostProcessResult onConfirmed(HandlerInput input, Workflow workflow) {
        ReservationRepositoryPort.CreateReservationRequest request = new ReservationRepositoryPort.CreateReservationRequest(
                workflow.collectedData().valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.DATE, SlotDataValue.DateValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.TIME, SlotDataValue.TimeValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.NumberValue.class).orElseThrow().value());
        ReservationRepositoryPort.ReservationResult result = reservationRepositoryPort.createReservation(request);
        if (!result.success()) {
            return WorkflowPostProcessResult.failure(Map.of(
                    "reason", result.message(),
                    "action_description", "The user wanted to create a reservation but it failed."));
        }
        Map<String, String> args = new java.util.HashMap<>(confirmationArguments(workflow));
        args.put("reference", result.referenceNumber());
        args.put("action_description", "The user has successfully created a reservation. The reference number is " + result.referenceNumber() + ".");
        return WorkflowPostProcessResult.success(Map.copyOf(args), result.referenceNumber());
    }

    @Override
    public Map<String, String> confirmationArguments(Workflow workflow) {
        return Map.of(
                "reservation_name", workflow.collectedData().valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class).orElseThrow().value(),
                "date", workflow.collectedData().valueAs(SlotName.DATE, SlotDataValue.DateValue.class).orElseThrow().value().toString(),
                "time", workflow.collectedData().valueAs(SlotName.TIME, SlotDataValue.TimeValue.class).orElseThrow().value().toString(),
                "people_count", Integer.toString(workflow.collectedData().valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.NumberValue.class).orElseThrow().value()),
                "action_description", "The user wants to create a new reservation. They need to confirm the details below.");
    }
}
