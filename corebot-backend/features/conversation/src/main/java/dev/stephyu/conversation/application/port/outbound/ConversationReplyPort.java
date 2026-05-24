package dev.stephyu.conversation.application.port.outbound;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Port for generating natural-language replies from structured conversation context.
 * The implementation uses an LLM to produce friendly, contextual responses
 * instead of static message templates.
 */
@NullMarked
public interface ConversationReplyPort {

    /**
     * Generates a natural-language reply.
     *
     * @param sessionId    conversation session id for memory isolation
     * @param language     ISO 639-1 language code (e.g. "fr", "en")
     * @param context      structured facts describing the outcome (e.g. reservation details, error reason)
     * @param userMessage  the original user message (for conversational acts like greetings/farewells)
     */
    String reply(String sessionId, String language, ReplyContext context, String userMessage);

    /**
     * Semantic intent communicated to the reply LLM so it knows what kind of response to produce,
     * independently of internal message-key naming conventions.
     */
    enum ReplyIntent {
        /** Ask the user for the next required piece of information (slot collection). */
        ASK_SLOT,
        /** All slots are collected — summarise them and ask the user to confirm or correct. */
        ASK_CONFIRMATION,
        /** The user wants to modify something — ask what they would like to change. */
        ASK_MODIFICATION,
        /** The workflow completed successfully. */
        WORKFLOW_SUCCESS,
        /** The workflow failed (e.g. reservation unavailable). */
        WORKFLOW_FAILURE,
        /** The workflow was cancelled at the user's request. */
        WORKFLOW_CANCELLED,
        /** The user's input was not understood or no matching workflow was found. */
        NOT_UNDERSTOOD,
        /** General conversational exchange (greetings, farewells, etc.). */
        GENERAL
    }

    /**
     * Describes the outcome of a workflow step to the reply LLM.
     *
     * @param replyIntent  semantic intent that tells the LLM what kind of reply to produce
     * @param facts        structured key-value facts about the outcome
     */
    record ReplyContext(ReplyIntent replyIntent, Map<String, String> facts) {
        public ReplyContext {
            facts = Map.copyOf(facts);
        }
    }
}

