package dev.stephyu.conversation.adapter.outbound.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ReservationToolsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldSerializeCapacityCheckResult() throws Exception {
        StubReservationRepository repository = new StubReservationRepository(
                new ReservationRepositoryPort.ReservationCapacityResult(
                        true,
                        "ok",
                        4,
                        120,
                        8,
                        2,
                        true,
                        List.of("T01", "T02"),
                        List.of()));

        ReservationTools tools = new ReservationTools(repository);
        String payload = tools.checkReservationCapacity("7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201", "2026-06-03", "13:00", 4);
        JsonNode json = OBJECT_MAPPER.readTree(payload);

        assertTrue(json.get("canReserve").asBoolean());
        assertEquals("ok", json.get("reason").asText());
        assertEquals(4, json.get("requestedPeopleCount").asInt());
        assertEquals(120, json.get("reservationDurationMinutes").asInt());
        assertEquals(8, json.get("availableSeats").asInt());
        assertEquals(2, json.get("requiredTables").asInt());
        assertTrue(json.get("canMergeTables").asBoolean());
        assertEquals("T01", json.get("selectedTableNumbers").get(0).asText());
    }

    @Test
    void shouldReturnStructuredFailureForInvalidDate() throws Exception {
        ReservationTools tools = new ReservationTools(new StubReservationRepository(null));
        String payload = tools.checkReservationCapacity("7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201", "bad-date", "13:00", 4);
        JsonNode json = OBJECT_MAPPER.readTree(payload);

        assertFalse(json.get("canReserve").asBoolean());
        assertEquals("invalid_date_or_time", json.get("reason").asText());
        assertEquals(4, json.get("requestedPeopleCount").asInt());
    }

    private static final class StubReservationRepository implements ReservationRepositoryPort {
        private final ReservationCapacityResult capacityResult;

        private StubReservationRepository(ReservationCapacityResult capacityResult) {
            this.capacityResult = capacityResult;
        }

        @Override
        public ReservationResult createReservation(CreateReservationRequest request) {
            return ReservationResult.failure("unsupported");
        }

        @Override
        public ReservationCapacityResult checkReservationCapacity(ReservationCapacityRequest request) {
            return capacityResult;
        }

        @Override
        public Optional<ReservationSummary> findReservation(String establishmentId, String referenceNumber, String channelUserId) {
            return Optional.empty();
        }

        @Override
        public ReservationResult cancelReservation(String establishmentId, String referenceNumber, String channelUserId) {
            return ReservationResult.failure("unsupported");
        }
    }
}
