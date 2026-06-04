package dev.stephyu.conversation.adapter.inbound.web;

import dev.stephyu.conversation.adapter.inbound.web.dto.SendMessageRequest;
import dev.stephyu.conversation.adapter.inbound.web.dto.SendMessageResponse;
import dev.stephyu.conversation.application.usecase.HandleConversationUseCase;
import static dev.stephyu.conversation.application.usecase.HandleConversationUseCase.HandleConversationCommand;
import static dev.stephyu.conversation.application.usecase.HandleConversationUseCase.HandleConversationResult;
import dev.stephyu.conversation.domain.EstablishmentId;
import dev.stephyu.conversation.domain.SessionId;
import dev.stephyu.conversation.domain.Channel;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SendMessageController {

    private final HandleConversationUseCase usecase;

    public SendMessageController(HandleConversationUseCase usecase) {
        this.usecase = usecase;
    }

    public void register(RoutesConfig routes) {
        routes.post("/api/v1/conversation/send_message", this::handleMessage);
    }

    public void handleMessage(Context ctx) {
        SendMessageRequest request = ctx.bodyValidator(SendMessageRequest.class)
                .check(req -> !req.message().isBlank(), "Message must not be empty")
                .check(req -> !req.establishmentId().isBlank(), "EstablishmentId must not be empty")
                .check(req -> !req.channelUserId().isBlank(), "ChannelUserId must not be empty")
                .get();

        var command = new HandleConversationCommand(
                resolveSessionId(request.sessionId()),
                EstablishmentId.of(request.establishmentId()),
                request.channelUserId(),
                resolveChannel(request.channel()),
                request.message());
        HandleConversationResult result = usecase.handle(command);
        var response = new SendMessageResponse(result.sessionId().value(), result.reply());

        ctx.status(HttpStatus.OK);
        ctx.json(response);
    }

    private SessionId resolveSessionId(@Nullable String rawSessionId) {
        if (rawSessionId == null || rawSessionId.isBlank()) {
            return SessionId.generate();
        }
        return SessionId.of(rawSessionId);
    }

    private static Channel resolveChannel(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Channel.UNKNOWN;
        }
        try {
            return Channel.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Channel.UNKNOWN;
        }
    }
}
