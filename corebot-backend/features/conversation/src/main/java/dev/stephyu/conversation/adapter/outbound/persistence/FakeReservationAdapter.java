package dev.stephyu.conversation.adapter.outbound.persistence;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FakeReservationAdapter implements ReservationRepositoryPort {

    private final Map<String, ReservationSummary> store = new HashMap<>();

    @Override
    public ReservationResult createReservation(CreateReservationRequest request) {
        String ref = generateReference();
        store.put(ref, new ReservationSummary(ref, request.reservationName(), request.date(), request.time(), request.peopleCount()));
        return ReservationResult.success(ref);
    }

    @Override
    public Optional<ReservationSummary> findReservation(String referenceNumber) {
        return Optional.ofNullable(store.get(referenceNumber.toUpperCase()));
    }

    @Override
    public ReservationResult cancelReservation(String referenceNumber) {
        String key = referenceNumber.toUpperCase();
        if (!store.containsKey(key)) {
            return ReservationResult.failure("not_found");
        }
        store.remove(key);
        return ReservationResult.success(key);
    }

    private static String generateReference() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
