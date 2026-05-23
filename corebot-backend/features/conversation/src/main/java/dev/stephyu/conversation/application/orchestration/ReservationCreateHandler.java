package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.port.outbound.ReservationPort;
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
            new SlotDefinition(SlotName.RESERVATION_NAME, true, "reservation_create.ask_reservation_name", List.of()),
            new SlotDefinition(SlotName.DATE, true, "reservation_create.ask_date", List.of()),
            new SlotDefinition(SlotName.TIME, true, "reservation_create.ask_time", List.of()),
            new SlotDefinition(SlotName.PEOPLE_COUNT, true, "reservation_create.ask_people_count", List.of()));

    private final ReservationPort reservationPort;

    public ReservationCreateHandler(ReservationPort reservationPort) {
        this.reservationPort = reservationPort;
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
        ReservationPort.CreateReservationRequest request = new ReservationPort.CreateReservationRequest(
                workflow.collectedData().valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.DATE, SlotDataValue.DateValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.TIME, SlotDataValue.TimeValue.class).orElseThrow().value(),
                workflow.collectedData().valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.NumberValue.class).orElseThrow().value());
        ReservationPort.ReservationResult result = reservationPort.createReservation(request);
        if (!result.success()) {
            return WorkflowPostProcessResult.failure("reservation_create.failure", Map.of("reason", result.message()));
        }
        return WorkflowPostProcessResult.success("reservation_create.success", confirmationArguments(workflow));
    }

    @Override
    public Map<String, String> confirmationArguments(Workflow workflow) {
        return Map.of(
                "reservation_name", workflow.collectedData().valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class).orElseThrow().value(),
                "date", workflow.collectedData().valueAs(SlotName.DATE, SlotDataValue.DateValue.class).orElseThrow().value().toString(),
                "time", workflow.collectedData().valueAs(SlotName.TIME, SlotDataValue.TimeValue.class).orElseThrow().value().toString(),
                "people_count", Integer.toString(workflow.collectedData().valueAs(SlotName.PEOPLE_COUNT, SlotDataValue.NumberValue.class).orElseThrow().value()));
    }
}
