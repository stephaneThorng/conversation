package dev.stephyu.conversation.application.reply;

import java.util.Locale;

public enum ResponseTone {
    FRIENDLY,
    JOYFUL,
    FORMAL,
    NEUTRAL;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
