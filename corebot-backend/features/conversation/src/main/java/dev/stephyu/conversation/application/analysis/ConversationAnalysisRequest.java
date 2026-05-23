package dev.stephyu.conversation.application.analysis;

import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.ConversationTurn;
import dev.stephyu.conversation.domain.workflow.WorkflowType;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConversationAnalysisRequest(
        String message,
        SessionId sessionId,
        EstablishmentId establishmentId,
        @Nullable WorkflowType activeWorkflowType,
        List<CollectedValueSnapshot> collectedData,
        List<String> missingRequiredSlots,
        boolean awaitingConfirmation,
        List<ConversationTurnSnapshot> recentTurns,
        @Nullable String language,
        String timezone,
        LocalDate referenceDate
) {

    public ConversationAnalysisRequest {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(establishmentId, "establishmentId must not be null");
        collectedData = List.copyOf(collectedData);
        missingRequiredSlots = List.copyOf(missingRequiredSlots);
        recentTurns = List.copyOf(recentTurns);
        Objects.requireNonNull(timezone, "timezone must not be null");
        Objects.requireNonNull(referenceDate, "referenceDate must not be null");
    }

    @NullMarked
    public record CollectedValueSnapshot(String slot, String type, String value) {

        public CollectedValueSnapshot {
            Objects.requireNonNull(slot, "slot must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    @NullMarked
    public record ConversationTurnSnapshot(ConversationTurn.Role role, String content) {

        public ConversationTurnSnapshot {
            Objects.requireNonNull(role, "role must not be null");
            Objects.requireNonNull(content, "content must not be null");
        }
    }
}
