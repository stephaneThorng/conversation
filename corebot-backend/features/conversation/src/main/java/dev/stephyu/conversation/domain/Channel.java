package dev.stephyu.conversation.domain;

import org.jspecify.annotations.NullMarked;

/**
 * Messaging channel through which the user is interacting.
 * The channel determines how the channelUserId is formatted
 * (phone number for WhatsApp, platform user ID for Telegram/Messenger, etc.)
 */
@NullMarked
public enum Channel {
    WHATSAPP,
    TELEGRAM,
    MESSENGER,
    WEB,
    UNKNOWN
}

