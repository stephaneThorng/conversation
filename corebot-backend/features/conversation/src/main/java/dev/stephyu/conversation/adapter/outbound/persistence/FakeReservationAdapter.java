package dev.stephyu.conversation.adapter.outbound.persistence;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FakeReservationAdapter implements ReservationRepositoryPort {

    // Stores full summary including channelUserId for ownership check
    private final Map<String, ReservationSummaryWithOwner> store = new HashMap<>();

    @Override
    public ReservationResult createReservation(CreateReservationRequest request) {
        String ref = generateReference();
        store.put(ref, new ReservationSummaryWithOwner(
                request.channelUserId(),
                new ReservationSummary(ref, request.reservationName(), request.date(), request.time(), request.peopleCount())));
        return ReservationResult.success(ref);
    }

    @Override
    public Optional<ReservationSummary> findReservation(String referenceNumber, String channelUserId) {
        ReservationSummaryWithOwner entry = store.get(referenceNumber.toUpperCase(java.util.Locale.ROOT));
        if (entry == null || !entry.channelUserId().equals(channelUserId)) {
            return Optional.empty();
        }
        return Optional.of(entry.summary());
    }

    @Override
    public ReservationResult cancelReservation(String referenceNumber, String channelUserId) {
        String key = referenceNumber.toUpperCase(java.util.Locale.ROOT);
        ReservationSummaryWithOwner entry = store.get(key);
        if (entry == null || !entry.channelUserId().equals(channelUserId)) {
            return ReservationResult.failure("not_found");
        }
        store.remove(key);
        return ReservationResult.success(key);
    }

    private static String generateReference() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private record ReservationSummaryWithOwner(String channelUserId, ReservationSummary summary) {}
}
