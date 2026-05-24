package dev.stephyu.conversation.adapter.outbound.reply;

import dev.stephyu.conversation.application.reply.ConversationReplyCatalog;
import dev.stephyu.conversation.application.reply.ResponseTone;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PropertiesConversationReplyCatalog implements ConversationReplyCatalog {

    @Override
    public String resolve(String language, ResponseTone responseTone, String messageKey, Map<String, String> arguments) {
        String template = findTemplate(language, responseTone, messageKey);
        String resolved = template;
        for (var entry : arguments.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resolved;
    }

    private static String findTemplate(String language, ResponseTone responseTone, String messageKey) {
        String normalizedLanguage = normalizeLanguage(language);
        Properties languageMessages = loadMessages(normalizedLanguage);
        Properties englishMessages = "en".equals(normalizedLanguage) ? languageMessages : loadMessages("en");
        String toneKey = responseTone.key();
        String[] candidates = {
                toneKey + "." + messageKey,
                "neutral." + messageKey
        };
        for (String candidate : candidates) {
            String template = languageMessages.getProperty(candidate);
            if (template != null) {
                return template;
            }
        }
        for (String candidate : candidates) {
            String template = englishMessages.getProperty(candidate);
            if (template != null) {
                return template;
            }
        }
        // Last resort: return the raw message key so the conversation doesn't crash
        return messageKey;
    }

    private static String normalizeLanguage(String language) {
        String normalized = Objects.requireNonNull(language, "language must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf('-');
        return separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : normalized;
    }

    private static Properties loadMessages(String language) {
        Properties properties = new Properties();
        String resourcePath = "messages_" + language + ".properties";
        try (InputStream inputStream = PropertiesConversationReplyCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return properties;
            }
            properties.load(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load message bundle " + resourcePath, exception);
        }
    }
}
