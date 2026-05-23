package dev.stephyu.conversation.application.reply;

import dev.stephyu.conversation.domain.EstablishmentId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EstablishmentResponseStyleResolver {

    ResponseTone resolve(EstablishmentId establishmentId);
}
