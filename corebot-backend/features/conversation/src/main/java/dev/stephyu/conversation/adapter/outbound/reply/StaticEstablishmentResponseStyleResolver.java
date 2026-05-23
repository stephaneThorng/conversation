package dev.stephyu.conversation.adapter.outbound.reply;

import dev.stephyu.conversation.application.reply.EstablishmentResponseStyleResolver;
import dev.stephyu.conversation.application.reply.ResponseTone;
import dev.stephyu.conversation.domain.EstablishmentId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StaticEstablishmentResponseStyleResolver implements EstablishmentResponseStyleResolver {

    @Override
    public ResponseTone resolve(EstablishmentId establishmentId) {
        return ResponseTone.FRIENDLY;
    }
}
