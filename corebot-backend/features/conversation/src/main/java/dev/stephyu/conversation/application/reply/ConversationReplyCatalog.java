package dev.stephyu.conversation.application.reply;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConversationReplyCatalog {

    String resolve(String language, ResponseTone responseTone, String messageKey, Map<String, String> arguments);
}
