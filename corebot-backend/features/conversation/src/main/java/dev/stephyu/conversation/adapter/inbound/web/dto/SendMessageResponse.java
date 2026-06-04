package dev.stephyu.conversation.adapter.inbound.web.dto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SendMessageResponse(String sessionId, String reply) {}
