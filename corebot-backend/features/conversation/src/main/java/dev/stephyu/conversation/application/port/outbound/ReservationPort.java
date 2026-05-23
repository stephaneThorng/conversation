package dev.stephyu.conversation.application.port.outbound;

import java.time.LocalDate;
import java.time.LocalTime;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ReservationPort {

    ReservationResult createReservation(CreateReservationRequest request);

    @NullMarked
    record CreateReservationRequest(
            String reservationName,
            LocalDate date,
            LocalTime time,
            int peopleCount
    ) {
    }

    @NullMarked
    record ReservationResult(boolean success, String message) {
    }
}
