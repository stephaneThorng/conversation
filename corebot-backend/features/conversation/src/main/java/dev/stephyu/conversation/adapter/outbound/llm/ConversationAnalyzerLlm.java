package dev.stephyu.conversation.adapter.outbound.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.stephyu.conversation.application.analysis.AnalyzedEntityType;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ConversationAnalyzerLlm {

    @SystemMessage("""
            You analyze user messages for a restaurant conversation workflow.
            Return only valid JSON.
            Detect the language, the intents and their entities.
            Always set language. Never return null or omit it.
            Use only one of these ISO 639-1 language codes: en, fr, de, es, it, pt, nl.
            If the language is genuinely unclear, use unknown.
            Supported workflow intent: RESERVATION_CREATE.
            Supported control intents: AFFIRMATIVE, NEGATIVE, CANCEL.
            Supported social intents: GREETING, THANKS, GOODBYE.
            Use entity types RESERVATION_NAME, DATE, TIME, PEOPLE_COUNT when relevant.
            Use conversation history only to understand what the latest USER message refers to.
            Extract entities only from the latest USER message.
            Do not repeat or copy collectedData values into the output unless the user restates or changes them in the current message.
            Use workflow context only to understand which slot the user is likely answering.
            If reservation_name is missing and the latest USER message is only a person name or a phrase like "au nom de Stephane", extract RESERVATION_NAME from that message.
            If the latest USER message contains a reservation intent together with a booking name and people count, extract all explicit entities from that same message.
            Do not use an intent phrase like "je veux reserver", "I want to book" or "reservation" as reservation_name.
            A user saying they want to reserve, book, reserver, reserve or make a booking is RESERVATION_CREATE, not NEGATIVE.
            NEGATIVE is only for explicit refusal or correction such as no, non, not this, or I do not confirm.
            Do not infer today's date just because a reservation workflow is active.
            Each entity must include a non-empty raw_value copied from the current user message.
            If you cannot provide a non-empty raw_value for an entity, omit that entity.
            Example:
            current user message: "Bonjour, je souhaite reserver"
            output intents: GREETING, RESERVATION_CREATE
            output entities: none
            Example:
            current user message: "Bonjour je veux reserver pour Richard pour 5 personnes"
            output language: fr
            output intents: RESERVATION_CREATE
            output entities: [{type: RESERVATION_NAME, raw_value: "Richard"}, {type: PEOPLE_COUNT, raw_value: "5"}]
            Example:
            missingRequiredSlots contains reservation_name
            conversation history:
            ASSISTANT: "Quel nom dois-je utiliser pour la reservation ?"
            current user message: "Stephane"
            output language: fr
            output intents: RESERVATION_CREATE
            output entities: [{type: RESERVATION_NAME, raw_value: "Stephane"}]
            Example:
            current user message: "Bonjour, je souhaite reserver"
            output language: fr
            Example:
            current user message: "Hello, I want to book"
            output language: en
            """)
    @UserMessage("""
            Analyze the current restaurant conversation and answer with structured data.

            Latest user message:
            {{message}}

            Conversation history:
            {{recentTurns}}

            Conversation language hint:
            {{language}}

            Workflow context:
            Active workflow type:
            {{activeWorkflowType}}

            Collected data:
            {{collectedData}}

            Missing required slots:
            {{missingRequiredSlots}}

            Awaiting confirmation:
            {{awaitingConfirmation}}
            """)
    ConversationAnalysisPayload analyze(
            @MemoryId String sessionId,
            @V("message") String message,
            @V("recentTurns") String recentTurns,
            @V("activeWorkflowType") String activeWorkflowType,
            @V("collectedData") String collectedData,
            @V("missingRequiredSlots") String missingRequiredSlots,
            @V("awaitingConfirmation") boolean awaitingConfirmation,
            @V("language") String language);

    @Description("Structured analysis of the latest user message in the restaurant conversation.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConversationAnalysisPayload(
            @Description("Detected ISO 639-1 language code of the user message. Must be one of: en, fr, de, es, it, pt, nl, unknown. Never null.")
            String language,
            @Description("Intents explicitly expressed by the latest user message, in the order they should be handled.")
            List<IntentPayload> intents
    ) {
        public ConversationAnalysisPayload {
            language = normalizeLanguage(language);
            intents = intents == null ? List.of() : List.copyOf(intents);
        }
    }

    @Description("One user intent detected in the latest message.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record IntentPayload(
            @Description("""
                    Intent name. Use RESERVATION_CREATE to start or continue a reservation workflow.
                    Use AFFIRMATIVE, NEGATIVE or CANCEL only for workflow control replies.
                    Use GREETING, THANKS or GOODBYE for simple social turns.
                    Use UNKNOWN only when no supported intent matches.
                    """)
            @Nullable AnalyzedIntentName name,
            @Description("Entities explicitly present in the latest user message for this intent.")
            List<EntityPayload> entities
    ) {
        public IntentPayload {
            entities = entities == null ? List.of() : List.copyOf(entities);
        }
    }

    @Description("One entity extracted from the latest user message.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EntityPayload(
            @Description("""
                    Entity type. Use RESERVATION_NAME for the booking name, DATE for reservation date,
                    TIME for reservation time, PEOPLE_COUNT for the number of guests.
                    Use MENU_NAME and MENU_ITEM_NAME only for menu-related questions.
                    Use UNKNOWN only when no supported entity type matches.
                    """)
            @Nullable AnalyzedEntityType type,
            @Description("Original extracted value exactly as expressed in the latest user message.")
            String raw_value
    ) {
    }

    private static String normalizeLanguage(@Nullable String language) {
        if (language == null || language.isBlank()) {
            return "unknown";
        }
        return language.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
