package dev.stephyu.conversation.application.port.outbound;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ReservationRepositoryPort {

    ReservationResult createReservation(CreateReservationRequest request);

    Optional<ReservationSummary> findReservation(String referenceNumber, String channelUserId);

    ReservationResult cancelReservation(String referenceNumber, String channelUserId);

    @NullMarked
    record CreateReservationRequest(
            String channelUserId,
            String reservationName,
            LocalDate date,
            LocalTime time,
            int peopleCount
    ) {
    }

    @NullMarked
    record ReservationResult(boolean success, String message, String referenceNumber) {
        public static ReservationResult success(String referenceNumber) {
            return new ReservationResult(true, "ok", referenceNumber);
        }
        public static ReservationResult failure(String message) {
            return new ReservationResult(false, message, "");
        }
    }

    @NullMarked
    record ReservationSummary(
            String referenceNumber,
            String reservationName,
            LocalDate date,
            LocalTime time,
            int peopleCount
    ) {
    }
}
