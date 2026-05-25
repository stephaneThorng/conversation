package dev.stephyu.conversation.adapter.outbound.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.stephyu.conversation.application.analysis.AnalyzedIntentName;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ConversationAnalyzerLlm {

    @SystemMessage("""
            You are a strict structured-data extractor for a restaurant assistant.
            Return only valid JSON matching the schema. Do not answer the user.
            Use only values explicitly present in the latest user message.
            Never invent slot values. Never copy slot values from history or collected data.
            Use history, activeWorkflowType, missingRequiredSlots, and awaitingConfirmation only to disambiguate intent.
            Reservation intent phrases like "reserve", "book", "booking", "reservation", "table for", and "reserver" mean RESERVATION_CREATE even if no slot value is present yet.
            Reservation lookup phrases mean RESERVATION_CHECK.
            Reservation cancellation phrases mean RESERVATION_CANCEL only when the user explicitly refers to an existing reservation.
            Social messages like greetings, thanks, farewells, or polite acknowledgements with no business action mean NONE.
            Pure confirmation means mainIntent=UNKNOWN with isAffirmative=true.
            Pure refusal without new data means mainIntent=UNKNOWN with isNegative=true.
            Use CANCEL only when the user wants to abort the current in-progress workflow.
            If there is no active workflow, a bare "cancel", "annuler", or "stop" is ambiguous: prefer UNKNOWN unless the message explicitly refers to a reservation.
            For menu intents, only classify the intent as ASK_MENU or ASK_MENU_ITEM. Do not extract menu filters.
            Absent fields must be null.

            Examples:
            message: "Bonjour je souhaite reserver"
            output: {"language":"fr","mainIntent":"RESERVATION_CREATE","isAffirmative":false,"isNegative":false,"isCancel":false}

            message: "I want to book"
            output: {"language":"en","mainIntent":"RESERVATION_CREATE","isAffirmative":false,"isNegative":false,"isCancel":false}

            message: "J'aimerais voir les details de ma reservation"
            output: {"language":"fr","mainIntent":"RESERVATION_CHECK","isAffirmative":false,"isNegative":false,"isCancel":false}

            missingRequiredSlots: reservation_name
            activeWorkflowType: RESERVATION_CREATE
            message: "Richard"
            output: {"language":"en","mainIntent":"RESERVATION_CREATE","isAffirmative":false,"isNegative":false,"isCancel":false,"reservationDetails":{"customerName":"Richard"}}

            awaitingConfirmation: true
            activeWorkflowType: RESERVATION_CREATE
            message: "je prefere le 26 mai"
            output: {"language":"fr","mainIntent":"RESERVATION_CREATE","isAffirmative":false,"isNegative":false,"isCancel":false,"reservationDetails":{"date":"le 26 mai"}}

            message: "merci"
            output: {"language":"fr","mainIntent":"NONE","isAffirmative":false,"isNegative":false,"isCancel":false}

            message: "oui c'est parfait"
            output: {"language":"fr","mainIntent":"UNKNOWN","isAffirmative":true,"isNegative":false,"isCancel":false}

            activeWorkflowType: RESERVATION_CREATE
            message: "annuler"
            output: {"language":"fr","mainIntent":"CANCEL","isAffirmative":false,"isNegative":false,"isCancel":true}

            activeWorkflowType: none
            message: "annuler"
            output: {"language":"fr","mainIntent":"UNKNOWN","isAffirmative":false,"isNegative":false,"isCancel":false}

            activeWorkflowType: none
            message: "je souhaite annuler ma reservation"
            output: {"language":"fr","mainIntent":"RESERVATION_CANCEL","isAffirmative":false,"isNegative":false,"isCancel":false}

            activeWorkflowType: none
            message: "I want to cancel my reservation"
            output: {"language":"en","mainIntent":"RESERVATION_CANCEL","isAffirmative":false,"isNegative":false,"isCancel":false}

            message: "quels plats vegan ?"
            output: {"language":"fr","mainIntent":"ASK_MENU_ITEM","isAffirmative":false,"isNegative":false,"isCancel":false}
            """)
    @UserMessage("""
            Extract structured data from the latest user message.

            Dynamic extraction guidance:
            {{analysisHints}}

            Latest user message:
            {{message}}

            Conversation history:
            {{recentTurns}}

            Conversation language hint:
            {{language}}

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
            @V("analysisHints") String analysisHints,
            @V("recentTurns") String recentTurns,
            @V("activeWorkflowType") String activeWorkflowType,
            @V("collectedData") String collectedData,
            @V("missingRequiredSlots") String missingRequiredSlots,
            @V("awaitingConfirmation") boolean awaitingConfirmation,
            @V("language") String language);

    @Description("Structured extraction of the latest user message for the restaurant assistant.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConversationAnalysisPayload(
            @Description("Detected ISO 639-1 language code. One of: en, fr, de, es, it, pt, nl, unknown. Never null.")
            String language,
            @Description("""
                    The single primary actionable intent of the latest user message.
                    Use RESERVATION_CREATE for any booking action or reservation slot correction.
                    Use RESERVATION_CHECK when the user wants to look up an existing reservation.
                    Use RESERVATION_CANCEL when the user wants to cancel an existing reservation.
                    Use NONE when the message is understood but has no actionable restaurant workflow intent,
                    for example greetings, thanks, farewells, or polite acknowledgements.
                    Use ASK_MENU when the user asks about a menu or menus.
                    Use ASK_MENU_ITEM when the user asks about dishes or menu items.
                    Use CANCEL when the user wants to abort the current in-progress workflow.
                    Use UNKNOWN for pure confirmations, pure refusals, or genuinely unrecognized messages.
                    Do not use AFFIRMATIVE or NEGATIVE here; use the boolean flags instead.
                    When the message is short or ambiguous and activeWorkflowType is not "none",
                    use activeWorkflowType as the intent.
                    """)
            @Nullable AnalyzedIntentName mainIntent,
            @Description("True when the user is confirming without new data. Independent of mainIntent.")
            boolean isAffirmative,
            @Description("True when the user is explicitly refusing without providing any new slot value. Independent of mainIntent.")
            boolean isNegative,
            @Description("True when the user wants to abort the current workflow. Set mainIntent = CANCEL too.")
            boolean isCancel,
            @Description("Reservation slot values extracted from the latest user message. Null when no reservation slot is explicitly present.")
            @Nullable ReservationDetailsPayload reservationDetails
    ) {
        public ConversationAnalysisPayload {
            language = normalizeLanguage(language);
        }
    }

    @Description("Reservation slot values extracted verbatim from the latest user message. Never invent or guess values.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationDetailsPayload(
            @Description("Person name for the booking, copied verbatim from the message. Null if absent.")
            @Nullable String customerName,
            @Description("Number of people as written in the message. Null if absent.")
            @Nullable String peopleCount,
            @Description("Date expression copied verbatim from the message. Null if absent.")
            @Nullable String date,
            @Description("Time expression copied verbatim from the message. Null if absent.")
            @Nullable String time,
            @Description("Reservation reference number copied from the message. Strip prefixes like 'reference' or 'ref'. Null if absent.")
            @Nullable String referenceNumber
    ) {
    }

    private static String normalizeLanguage(@Nullable String language) {
        if (language == null || language.isBlank()) {
            return "unknown";
        }
        return language.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
