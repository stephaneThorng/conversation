package dev.stephyu.conversation.domain;

import dev.stephyu.conversation.domain.workflow.Workflow;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConversationState(
        EstablishmentId establishmentId,
        @Nullable Workflow workflow,
        @Nullable String lastAssistantReply
) {

    public ConversationState {
        Objects.requireNonNull(establishmentId, "establishmentId must not be null");
    }

    public ConversationState(EstablishmentId establishmentId, @Nullable Workflow workflow) {
        this(establishmentId, workflow, null);
    }

    public Optional<Workflow> activeWorkflow() {
        return Optional.ofNullable(workflow);
    }

    public boolean hasActiveWorkflow() {
        return workflow != null;
    }

    public ConversationState withWorkflow(Workflow nextWorkflow) {
        return new ConversationState(establishmentId, nextWorkflow, lastAssistantReply);
    }

    public ConversationState withoutWorkflow() {
        return new ConversationState(establishmentId, null, lastAssistantReply);
    }

    public ConversationState withLastAssistantReply(String reply) {
        return new ConversationState(establishmentId, workflow, reply);
    }
}
