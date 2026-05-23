package dev.stephyu.conversation.adapter.outbound.reply;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stephyu.conversation.application.reply.ResponseTone;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesConversationReplyCatalogTest {

    @Test
    void resolvesTemplateWithPreferredLanguageAndTone() {
        PropertiesConversationReplyCatalog catalog = new PropertiesConversationReplyCatalog();

        String reply = catalog.resolve(
                "fr",
                ResponseTone.FRIENDLY,
                "reservation_create.ask_date",
                Map.of());

        assertEquals("Pour quelle date souhaitez-vous reserver ?", reply);
    }

    @Test
    void fallsBackToEnglishNeutral() {
        PropertiesConversationReplyCatalog catalog = new PropertiesConversationReplyCatalog();

        String reply = catalog.resolve(
                "it",
                ResponseTone.JOYFUL,
                "workflow.not_understood",
                Map.of());

        assertEquals("I did not understand that request.", reply);
    }
}
