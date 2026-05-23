package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.ConversationSession;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record HandlerInput(
        ConversationSession session,
        String message,
        List<AnalyzedEntity> entities,
        boolean affirmative,
        boolean negative,
        String language,
        ResponseTone responseTone
) {

    public HandlerInput {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(message, "message must not be null");
        entities = List.copyOf(entities);
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(responseTone, "responseTone must not be null");
    }
}
