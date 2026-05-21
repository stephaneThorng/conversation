package dev.stephyu.conversation.domain.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.domain.slot.CollectedData;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotDefinition;
import dev.stephyu.conversation.domain.slot.SlotName;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void detectsMissingRequiredSlots() {
        Workflow workflow = reservationWorkflow(CollectedData.empty(), false);

        assertEquals(2, workflow.missingRequiredSlots().size());
        assertFalse(workflow.isReadyForConfirmation());
    }

    @Test
    void isReadyForConfirmationWhenRequiredSlotsAreCollected() {
        CollectedData data = CollectedData.empty()
                .withValue(SlotName.RESERVATION_NAME, new SlotDataValue.TextValue("Stephane"))
                .withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.NumberValue(2));
        Workflow workflow = reservationWorkflow(data, false);

        assertTrue(workflow.missingRequiredSlots().isEmpty());
        assertTrue(workflow.isReadyForConfirmation());
        assertTrue(workflow.isAwaitingConfirmation());
    }

    @Test
    void confirmedWorkflowDoesNotAwaitConfirmationAnymore() {
        CollectedData data = CollectedData.empty()
                .withValue(SlotName.RESERVATION_NAME, new SlotDataValue.TextValue("Stephane"))
                .withValue(SlotName.PEOPLE_COUNT, new SlotDataValue.NumberValue(2));
        Workflow workflow = reservationWorkflow(data, false).confirm();

        assertTrue(workflow.confirmed());
        assertFalse(workflow.isReadyForConfirmation());
        assertFalse(workflow.isAwaitingConfirmation());
    }

    private static Workflow reservationWorkflow(CollectedData data, boolean confirmed) {
        return new Workflow(
                WorkflowType.RESERVATION_CREATE,
                List.of(
                        new SlotDefinition(
                                SlotName.RESERVATION_NAME,
                                true,
                                "reservation.name.prompt",
                                List.of()),
                        new SlotDefinition(
                                SlotName.PEOPLE_COUNT,
                                true,
                                "reservation.people.prompt",
                                List.of()),
                        new SlotDefinition(
                                SlotName.DATE,
                                false,
                                "reservation.date.prompt",
                                List.of())),
                data,
                confirmed);
    }
}
