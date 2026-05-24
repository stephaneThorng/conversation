package dev.stephyu.conversation.domain;

import dev.stephyu.conversation.domain.workflow.Workflow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConversationState(
        EstablishmentId establishmentId,
        @Nullable String language,
        @Nullable Workflow workflow,
        List<ConversationTurn> recentTurns,
        @Nullable String lastReservationReference
) {

    private static final int MAX_RECENT_TURNS = 6;

    public ConversationState {
        Objects.requireNonNull(establishmentId, "establishmentId must not be null");
        recentTurns = boundedTurns(recentTurns);
    }

    public ConversationState(EstablishmentId establishmentId, @Nullable Workflow workflow) {
        this(establishmentId, null, workflow, List.of(), null);
    }

    public Optional<Workflow> activeWorkflow() {
        return Optional.ofNullable(workflow);
    }

    public boolean hasActiveWorkflow() {
        return workflow != null;
    }

    public Optional<String> reservationReference() {
        return Optional.ofNullable(lastReservationReference);
    }

    public ConversationState withWorkflow(Workflow nextWorkflow) {
        return new ConversationState(establishmentId, language, nextWorkflow, recentTurns, lastReservationReference);
    }

    public ConversationState withoutWorkflow() {
        return new ConversationState(establishmentId, language, null, recentTurns, lastReservationReference);
    }

    public ConversationState withReservationReference(String reference) {
        return new ConversationState(establishmentId, language, workflow, recentTurns, reference);
    }

    public ConversationState withLastAssistantReply(String reply) {
        return withAppendedTurn(new ConversationTurn(ConversationTurn.Role.ASSISTANT, reply));
    }

    public ConversationState withLanguage(String nextLanguage) {
        return new ConversationState(establishmentId, nextLanguage, workflow, recentTurns, lastReservationReference);
    }

    public ConversationState withUserMessage(String message) {
        return withAppendedTurn(new ConversationTurn(ConversationTurn.Role.USER, message));
    }

    public Optional<String> lastAssistantReply() {
        for (int index = recentTurns.size() - 1; index >= 0; index--) {
            ConversationTurn turn = recentTurns.get(index);
            if (turn.role() == ConversationTurn.Role.ASSISTANT) {
                return Optional.of(turn.content());
            }
        }
        return Optional.empty();
    }

    private ConversationState withAppendedTurn(ConversationTurn turn) {
        List<ConversationTurn> nextTurns = new ArrayList<>(recentTurns);
        nextTurns.add(turn);
        return new ConversationState(establishmentId, language, workflow, nextTurns, lastReservationReference);
    }

    private static List<ConversationTurn> boundedTurns(List<ConversationTurn> turns) {
        Objects.requireNonNull(turns, "recentTurns must not be null");
        if (turns.size() <= MAX_RECENT_TURNS) {
            return List.copyOf(turns);
        }
        int fromIndex = turns.size() - MAX_RECENT_TURNS;
        return Collections.unmodifiableList(new ArrayList<>(turns.subList(fromIndex, turns.size())));
    }
}
