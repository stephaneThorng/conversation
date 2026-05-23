package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.application.port.outbound.ReservationPort;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReservationCreateHandlerTest {

    @Test
    void exposesReservationWorkflowConfiguration() {
        ReservationCreateHandler handler = new ReservationCreateHandler(
                request -> new ReservationPort.ReservationResult(true, "ok"));

        Workflow workflow = handler.newWorkflow();

        assertEquals(WorkflowType.RESERVATION_CREATE, handler.workflowType());
        assertEquals(4, workflow.slotDefinitions().size());
        assertEquals(SlotName.RESERVATION_NAME, workflow.slotDefinitions().get(0).name());
        assertEquals(SlotName.DATE, workflow.slotDefinitions().get(1).name());
        assertEquals(SlotName.TIME, workflow.slotDefinitions().get(2).name());
        assertEquals(SlotName.PEOPLE_COUNT, workflow.slotDefinitions().get(3).name());
    }

    @Test
    void buildsConfirmationArgumentsFromCollectedData() {
        ReservationCreateHandler handler = new ReservationCreateHandler(
                request -> new ReservationPort.ReservationResult(true, "ok"));

        Map<String, String> arguments = handler.confirmationArguments(completeWorkflow());

        assertEquals("Martin", arguments.get("reservation_name"));
        assertEquals("2026-05-24", arguments.get("date"));
        assertEquals("18:00", arguments.get("time"));
        assertEquals("4", arguments.get("people_count"));
    }

    @Test
    void callsReservationPortWhenConfirmed() {
        AtomicReference<ReservationPort.CreateReservationRequest> capturedRequest = new AtomicReference<>();
        ReservationCreateHandler handler = new ReservationCreateHandler(request -> {
            capturedRequest.set(request);
            return new ReservationPort.ReservationResult(true, "ok");
        });

        WorkflowPostProcessResult result = handler.onConfirmed(TestFixtures.handlerInput(), completeWorkflow());

        assertTrue(result.success());
        assertEquals("reservation_create.success", result.messageKey());
        assertEquals("Martin", capturedRequest.get().reservationName());
        assertEquals(LocalDate.of(2026, 5, 24), capturedRequest.get().date());
        assertEquals(LocalTime.of(18, 0), capturedRequest.get().time());
        assertEquals(4, capturedRequest.get().peopleCount());
    }

    private static Workflow completeWorkflow() {
        CollectedData data = CollectedData.empty()
                .withValue(SlotName.RESERVATION_NAME, new SlotDataValue.TextValue("Martin"))
                .withValue(SlotName.DATE, new SlotDataValue.DateValue(LocalDate.of(2026, 5, 24)))
                .withValue(SlotName.TIME, new SlotDataValue.TimeValue(LocalTime.of(18, 0)))
                .withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.NumberValue(4));
        return new Workflow(
                WorkflowType.RESERVATION_CREATE,
                new ReservationCreateHandler(request -> new ReservationPort.ReservationResult(true, "ok"))
                        .newWorkflow()
                        .slotDefinitions(),
                data,
                false);
    }
}
