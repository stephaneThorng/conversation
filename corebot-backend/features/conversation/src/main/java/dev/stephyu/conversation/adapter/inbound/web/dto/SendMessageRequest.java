package dev.stephyu.conversation.adapter.inbound.web.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SendMessageRequest (@Nullable String sessionId, String message) {}

