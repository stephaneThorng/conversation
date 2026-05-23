package dev.stephyu.conversation.adapter.outbound.persistence;

import dev.stephyu.conversation.application.port.outbound.ReservationPort;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FakeReservationAdapter implements ReservationPort {

    @Override
    public ReservationResult createReservation(CreateReservationRequest request) {
        return new ReservationResult(true, "ok");
    }
}
