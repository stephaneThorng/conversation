package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ReservationCreateHandlerTest {

    @Test
    void exposesReservationWorkflowConfiguration() {
        ReservationCreateHandler handler = new ReservationCreateHandler(new FakeRepo(true));

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
        ReservationCreateHandler handler = new ReservationCreateHandler(new FakeRepo(true));

        Map<String, String> arguments = handler.confirmationArguments(completeWorkflow());

        assertEquals("Martin", arguments.get("reservation_name"));
        assertEquals("2026-05-24", arguments.get("date"));
        assertEquals("18:00", arguments.get("time"));
        assertEquals("4", arguments.get("people_count"));
    }

    @Test
    void callsReservationPortWhenConfirmed() {
        AtomicReference<ReservationRepositoryPort.CreateReservationRequest> capturedRequest = new AtomicReference<>();
        ReservationCreateHandler handler = new ReservationCreateHandler(new ReservationRepositoryPort() {
            @Override
            public ReservationRepositoryPort.ReservationResult createReservation(ReservationRepositoryPort.CreateReservationRequest request) {
                capturedRequest.set(request);
                return ReservationRepositoryPort.ReservationResult.success("REF001");
            }
            @Override
            public java.util.Optional<ReservationRepositoryPort.ReservationSummary> findReservation(String ref) {
                return java.util.Optional.empty();
            }
            @Override
            public ReservationRepositoryPort.ReservationResult cancelReservation(String ref) {
                return ReservationRepositoryPort.ReservationResult.failure("not_found");
            }
        });

        WorkflowPostProcessResult result = handler.onConfirmed(TestFixtures.handlerInput(), completeWorkflow());

        assertTrue(result.success());
        assertEquals("reservation_create.success", result.messageKey());
        assertEquals("REF001", result.arguments().get("reference"));
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
                new ReservationCreateHandler(new FakeRepo(true)).newWorkflow().slotDefinitions(),
                data,
                false);
    }

    private record FakeRepo(boolean success) implements ReservationRepositoryPort {
        @Override
        public ReservationResult createReservation(CreateReservationRequest request) {
            return success ? ReservationResult.success("FAKEREF") : ReservationResult.failure("full");
        }
        @Override
        public java.util.Optional<ReservationSummary> findReservation(String ref) {
            return java.util.Optional.empty();
        }
        @Override
        public ReservationResult cancelReservation(String ref) {
            return ReservationResult.failure("not_found");
        }
    }
}
