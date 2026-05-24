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
            You are a data-extraction engine for a restaurant booking assistant.
            Your only job is to fill the JSON schema below — nothing else.
            Never add commentary. Return only valid JSON that matches the schema.

            ── FEW-SHOT EXAMPLES ──

            Example 1 — booking intent only, NO slot data → all reservation fields must be null:
            message: "Bonjour je souhaite réserver"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false
            }

            Example 2 — booking with name and people count (ignore greeting/thanks):
            message: "Bonjour ! Je veux réserver au nom de Stephane pour 5 personnes, merci !"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "customerName": "Stephane",
                "peopleCount": "5",
              }
            }

            Example 3 — single slot answer (name):
            missingRequiredSlots: reservation_name
            previous assistant message: "Quel nom dois-je utiliser pour la réservation ?"
            message: "richard"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "customerName": "richard"
              }
            }

            Example 4 — date and time in the same message:
            missingRequiredSlots: date, time
            message: "dans 2 jours à 20h30"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "date": "dans 2 jours",
                "time": "20h30"
              }
            }

            Example 4b — short ambiguous message (bare date) with active workflow → inherit activeWorkflowType:
            activeWorkflowType: RESERVATION_CREATE
            missingRequiredSlots: date
            message: "lundi prochain"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "date": "lundi prochain"
              }
            }

            Example 5 — user confirms (no new data):
            message: "oui c'est parfait"
            output:
            {
              "language": "fr",
              "mainIntent": "UNKNOWN",
              "isAffirmative": true,
              "isNegative": false,
              "isCancel": false
            }

            Example 6 — user refuses without new data:
            message: "non"
            output:
            {
              "language": "fr",
              "mainIntent": "UNKNOWN",
              "isAffirmative": false,
              "isNegative": true,
              "isCancel": false
            }

            Example 7 — user refuses AND provides a new date:
            awaitingConfirmation: true
            collectedData: date=2026-05-25, reservation_name=Stephane, people_count=15, time=19:30
            message: "je préfère plutôt pour le 26 mai"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "date": "le 26 mai"
              }
            }

            Example 8 — user provides only a corrected date:
            awaitingConfirmation: true
            collectedData: date=2026-05-25, reservation_name=Stephane, people_count=15, time=19:30
            message: "le 26 mai"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "date": "le 26 mai"
              }
            }

            Example 9 — user cancels:
            message: "annuler"
            output:
            {
              "language": "fr",
              "mainIntent": "CANCEL",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": true
            }

            Example 10 — single slot answer (people count):
            activeWorkflowType: RESERVATION_CREATE
            missingRequiredSlots: people_count
            message: "5"
            output:
            {
              "language": "en",
              "mainIntent": "RESERVATION_CREATE",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "peopleCount": "5"
              }
            }

            Example 11 — user provides a reservation reference number:
            activeWorkflowType: RESERVATION_CHECK
            missingRequiredSlots: reference_number
            message: "1EBC495E"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CHECK",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "referenceNumber": "1EBC495E"
              }
            }

            Example 12 — user provides a reference number with prefix:
            activeWorkflowType: RESERVATION_CHECK
            missingRequiredSlots: reference_number
            message: "reference 1EBC495E"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CHECK",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "referenceNumber": "1EBC495E"
              }
            }

            Example 13 — user wants to cancel a reservation:
            message: "je souhaite annuler ma réservation"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CANCEL",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false
            }

            Example 14 — user provides a reference number for cancellation:
            activeWorkflowType: RESERVATION_CANCEL
            missingRequiredSlots: reference_number
            message: "1EBC495E"
            output:
            {
              "language": "fr",
              "mainIntent": "RESERVATION_CANCEL",
              "isAffirmative": false,
              "isNegative": false,
              "isCancel": false,
              "reservationDetails": {
                "referenceNumber": "1EBC495E"
              }
            }
            """)
    @UserMessage("""
            Extract structured data from the latest user message.

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
                    Use RESERVATION_CREATE for any booking action or slot correction.
                    Use RESERVATION_CHECK when the user wants to look up an existing reservation.
                    Use RESERVATION_CANCEL when the user wants to cancel an existing reservation.
                    Use CANCEL when the user wants to abort the current in-progress workflow.
                    Use UNKNOWN for pure confirmations, pure refusals, or unrecognized messages.
                    Do NOT use AFFIRMATIVE or NEGATIVE here — use the boolean flags instead.
                    When the message is short or ambiguous (e.g. a bare date, a name, a number, a time)
                    and activeWorkflowType is set and not "none", use that workflow type as the intent.
                    """)
            @Nullable AnalyzedIntentName mainIntent,
            @Description("True when the user is confirming without new data (yes, oui, ok, d'accord, parfait …). Independent of mainIntent.")
            boolean isAffirmative,
            @Description("True when the user is explicitly refusing without providing any new slot value (no, non, pas ça …). Independent of mainIntent.")
            boolean isNegative,
            @Description("True when the user wants to abort the current workflow (cancel, annuler, stop …). Set mainIntent = CANCEL too.")
            boolean isCancel,
            @Description("Reservation slot values extracted from the latest user message. Set to null when mainIntent is not RESERVATION_CREATE or RESERVATION_CHECK.")
            @Nullable ReservationDetailsPayload reservationDetails
    ) {
        public ConversationAnalysisPayload {
            language = normalizeLanguage(language);
        }
    }

    @Description("Reservation slot values extracted verbatim from the latest user message. NEVER invent or guess values. Every field must be null if not explicitly present in the message.")
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationDetailsPayload(
            @Description("Person name for the booking, copied verbatim from the message (e.g. 'Richard', 'Martin'). NULL if no name is explicitly written in the message. NEVER invent a name.")
            @Nullable String customerName,
            @Description("Number of guests as written in the message (e.g. '5', 'deux', 'two'). NULL if no people count is explicitly written in the message.")
            @Nullable String peopleCount,
            @Description("Date expression copied verbatim from the message (e.g. 'lundi prochain', 'dans 2 jours', 'dimanche 24 mai 2026'). NULL if no date is explicitly written in the message.")
            @Nullable String date,
            @Description("Time expression copied verbatim from the message (e.g. '20h30', '8pm', 'ce soir'). NULL if no time is explicitly written in the message.")
            @Nullable String time,
            @Description("Reservation reference number copied verbatim from the message (e.g. '1EBC495E', 'ABC12345'). Extract only the alphanumeric code, strip any prefix like 'reference' or 'ref'. NULL if not present.")
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
