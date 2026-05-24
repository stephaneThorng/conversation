package dev.stephyu.conversation.application.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.adapter.outbound.normalizer.RecognizersTextSlotValueNormalizer;
import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import dev.stephyu.conversation.domain.ConversationState;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import dev.stephyu.conversation.domain.workflow.Workflow;
import org.junit.jupiter.api.Test;
import java.util.List;

class WorkflowProcessorTest {

    private final WorkflowProcessor processor = new WorkflowProcessor(new RecognizersTextSlotValueNormalizer());

    @Test
    void startsWorkflowAndAsksFirstMissingSlot() {
        ReplyDirective directive = processor.process(inputWithEntities(
                List.of(new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin"))),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.ask_date", directive.messageKey());
    }

    @Test
    void appliesSeveralEntitiesAndAsksNextMissingSlot() {
        ReplyDirective directive = processor.process(inputWithEntities(List.of(
                        new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin"),
                        new AnalyzedEntity(AnalyzedEntityType.DATE, "tomorrow"))),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.ask_time", directive.messageKey());
    }

    @Test
    void asksForConfirmationWhenAllSlotsAreCollected() {
        ReplyDirective directive = processor.process(inputWithEntities(completeEntities()), successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.confirmation_summary", directive.messageKey());
        assertEquals("Martin", directive.arguments().get("reservation_name"));
        assertEquals("4", directive.arguments().get("people_count"));
    }

    @Test
    void doesNotConfirmWhenAffirmativeAlsoCarriesEntityUpdates() {
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(workflowMissingGuestCount());
        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "10 personnes",
                        List.of(new AnalyzedEntity(AnalyzedEntityType.PEOPLE_COUNT, "10 personnes")),
                        true,
                        false,
                        "fr",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.confirmation_summary", directive.messageKey());
        assertEquals("10", directive.arguments().get("people_count"));
    }

    @Test
    void confirmsOnlyWhenWorkflowIsCompleteAndAffirmativeHasNoEntityUpdates() {
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(completeWorkflow());
        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "yes",
                        List.of(),
                        true,
                        false,
                        "en",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertFalse(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.success", directive.messageKey());
    }

    @Test
    void keepsWorkflowActiveOnNegative() {
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(completeWorkflow());
        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "no",
                        List.of(),
                        false,
                        true,
                        "en",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.modify_prompt", directive.messageKey());
    }

    @Test
    void keepsWorkflowActiveWhenPostProcessFails() {
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(completeWorkflow());
        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "yes",
                        List.of(),
                        true,
                        false,
                        "en",
                        ResponseTone.FRIENDLY),
                new ReservationCreateHandler(new FakeRepo(false, "full")));

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.failure", directive.messageKey());
        assertEquals("full", directive.arguments().get("reason"));
    }

    @Test
    void infersReservationNameWhenAnalyzerMissesIt() {
        Workflow workflow = successfulHandler().newWorkflow()
                .withCollectedData(successfulHandler().newWorkflow().collectedData()
                        .withValue(SlotName.DATE, new SlotDataValue.DateValue(java.time.LocalDate.of(2026, 5, 24))));
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(workflow);

        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "au nom de Stephane",
                        List.of(),
                        false,
                        false,
                        "fr",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.ask_time", directive.messageKey());
        assertEquals(
                "Stephane",
                directive.state()
                        .activeWorkflow()
                        .orElseThrow()
                        .collectedData()
                        .valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class)
                        .orElseThrow()
                        .value());
    }

    @Test
    void ignoresReservationIntentTextAsReservationName() {
        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                        "je veux reserver",
                        List.of(new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "je veux reserver")),
                        false,
                        false,
                        "fr",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertTrue(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.ask_reservation_name", directive.messageKey());
        assertTrue(directive.state().activeWorkflow().orElseThrow()
                .collectedData()
                .valueAs(SlotName.RESERVATION_NAME, SlotDataValue.TextValue.class)
                .isEmpty());
    }

    @Test
    void confirmsWhenAffirmativeCarriesOnlyCopiedCollectedValues() {
        Workflow workflow = completeWorkflow();
        ConversationState state = new ConversationState(EstablishmentId.of("est-1"), null)
                .withWorkflow(workflow);

        ReplyDirective directive = processor.process(new HandlerInput(
                        new ConversationSession(SessionId.of("session-1"), state),
                        "oui",
                        completeEntities(),
                        true,
                        false,
                        "fr",
                        ResponseTone.FRIENDLY),
                successfulHandler());

        assertFalse(directive.state().hasActiveWorkflow());
        assertEquals("reservation_create.success", directive.messageKey());
    }

    private static ReservationCreateHandler successfulHandler() {
        return new ReservationCreateHandler(new FakeRepo(true, null));
    }

    private static HandlerInput inputWithEntities(List<AnalyzedEntity> entities) {
        return new HandlerInput(
                new ConversationSession(SessionId.of("session-1"), new ConversationState(EstablishmentId.of("est-1"), null)),
                "message",
                entities,
                false,
                false,
                "en",
                ResponseTone.FRIENDLY);
    }

    private static List<AnalyzedEntity> completeEntities() {
        return List.of(
                new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin"),
                new AnalyzedEntity(AnalyzedEntityType.DATE, "tomorrow"),
                new AnalyzedEntity(AnalyzedEntityType.TIME, "6pm"),
                new AnalyzedEntity(AnalyzedEntityType.PEOPLE_COUNT, "four"));
    }

    private static Workflow completeWorkflow() {
        return processorForFixture().process(inputWithEntities(completeEntities()), successfulHandler())
                .state()
                .activeWorkflow()
                .orElseThrow();
    }

    private static Workflow workflowMissingGuestCount() {
        return processorForFixture().process(inputWithEntities(List.of(
                        new AnalyzedEntity(AnalyzedEntityType.RESERVATION_NAME, "Martin"),
                        new AnalyzedEntity(AnalyzedEntityType.DATE, "tomorrow"),
                        new AnalyzedEntity(AnalyzedEntityType.TIME, "6pm"))),
                successfulHandler())
                .state()
                .activeWorkflow()
                .orElseThrow();
    }

    private static WorkflowProcessor processorForFixture() {
        return new WorkflowProcessor(new RecognizersTextSlotValueNormalizer());
    }

    private record FakeRepo(boolean success, @org.jspecify.annotations.Nullable String failReason) implements ReservationRepositoryPort {
        @Override
        public ReservationResult createReservation(CreateReservationRequest request) {
            return success ? ReservationResult.success("TESTREF") : ReservationResult.failure(failReason != null ? failReason : "error");
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
