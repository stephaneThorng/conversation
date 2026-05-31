package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_TABLE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PostgresReservationRepositoryTest extends PostgresReservationRepositoryTestSupport {

    @Test
    void shouldCreateFindAndCancelReservation() {
        ReservationSlot slot = nextReservableSlot();

        ReservationRepositoryPort.ReservationResult created =
                repository.createReservation(request(slot.date(), slot.time(), 2));

        assertTrue(created.success());
        assertNotNull(created.referenceNumber());

        Optional<ReservationRepositoryPort.ReservationSummary> found =
                repository.findReservation(ESTABLISHMENT_ID.toString(), created.referenceNumber(), "channel-user-1");
        assertTrue(found.isPresent());
        assertEquals(slot.date(), found.get().date());
        assertEquals(slot.time(), found.get().time());

        Optional<ReservationRepositoryPort.ReservationSummary> otherUser =
                repository.findReservation(ESTABLISHMENT_ID.toString(), created.referenceNumber(), "other-user");
        assertTrue(otherUser.isEmpty());

        ReservationRepositoryPort.ReservationResult cancelled =
                repository.cancelReservation(ESTABLISHMENT_ID.toString(), created.referenceNumber(), "channel-user-1");
        assertTrue(cancelled.success());

        String status = dsl.fetchValue(
                dsl.select(RESTAURANT_RESERVATION.STATUS)
                        .from(RESTAURANT_RESERVATION)
                        .where(RESTAURANT_RESERVATION.REFERENCE_NUMBER.eq(created.referenceNumber())));
        assertEquals("CANCELLED", status);
        assertTrue(repository.findReservation(ESTABLISHMENT_ID.toString(), created.referenceNumber(), "channel-user-1").isEmpty());
    }

    @Test
    void shouldAllocateMergedTablesWhenRequired() {
        ReservationSlot slot = nextReservableSlot();

        ReservationRepositoryPort.ReservationResult created =
                repository.createReservation(request(slot.date(), slot.time(), 9));

        assertTrue(created.success());

        List<String> tables = dsl.select(RESTAURANT_TABLE.TABLE_NUMBER)
                .from(RESTAURANT_RESERVATION_TABLE)
                .join(RESTAURANT_TABLE)
                .on(RESTAURANT_TABLE.ID.eq(RESTAURANT_RESERVATION_TABLE.TABLE_ID))
                .join(RESTAURANT_RESERVATION)
                .on(RESTAURANT_RESERVATION.ID.eq(RESTAURANT_RESERVATION_TABLE.RESERVATION_ID))
                .where(RESTAURANT_RESERVATION.REFERENCE_NUMBER.eq(created.referenceNumber()))
                .orderBy(RESTAURANT_TABLE.TABLE_NUMBER.asc())
                .fetch(record -> record.get(RESTAURANT_TABLE.TABLE_NUMBER));

        assertEquals(List.of("T01", "T09"), tables);
    }

    @Test
    void shouldReportDirectTableCapacity() {
        ReservationSlot slot = nextReservableSlot();

        ReservationRepositoryPort.ReservationCapacityResult result = repository.checkReservationCapacity(
                new ReservationRepositoryPort.ReservationCapacityRequest(
                        ESTABLISHMENT_ID.toString(),
                        slot.date(),
                        slot.time(),
                        2));

        assertTrue(result.canReserve());
        assertEquals("ok", result.reason());
        assertEquals(2, result.requestedPeopleCount());
        assertEquals(120, result.reservationDurationMinutes());
        assertEquals(2, result.availableSeats());
        assertEquals(1, result.requiredTables());
        assertTrue(result.canMergeTables());
        assertEquals(List.of("T01"), result.selectedTableNumbers());
    }

    @Test
    void shouldReportMergedTableCapacity() {
        ReservationSlot slot = nextReservableSlot();

        ReservationRepositoryPort.ReservationCapacityResult result = repository.checkReservationCapacity(
                new ReservationRepositoryPort.ReservationCapacityRequest(
                        ESTABLISHMENT_ID.toString(),
                        slot.date(),
                        slot.time(),
                        9));

        assertTrue(result.canReserve());
        assertEquals("ok", result.reason());
        assertEquals(9, result.requestedPeopleCount());
        assertEquals(120, result.reservationDurationMinutes());
        assertEquals(10, result.availableSeats());
        assertEquals(2, result.requiredTables());
        assertTrue(result.canMergeTables());
        assertEquals(List.of("T01", "T09"), result.selectedTableNumbers());
    }

    @Test
    void shouldReportNoCapacityWhenTablesAreAlreadyOccupied() {
        ReservationSlot slot = nextReservableSlot();
        occupyAllTables(slot.date(), slot.time());

        ReservationRepositoryPort.ReservationCapacityResult result = repository.checkReservationCapacity(
                new ReservationRepositoryPort.ReservationCapacityRequest(
                        ESTABLISHMENT_ID.toString(),
                        slot.date(),
                        slot.time(),
                        2));

        assertFalse(result.canReserve());
        assertEquals("no_capacity", result.reason());
        assertEquals(2, result.requestedPeopleCount());
        assertEquals(0, result.requiredTables());
        assertEquals(0, result.availableSeats());
    }

    @Test
    void shouldRejectReservationsOnClosedDay() {
        LocalDate monday = nextDateFor(DayOfWeek.MONDAY);

        ReservationRepositoryPort.ReservationResult result =
                repository.createReservation(request(monday, LocalTime.of(19, 0), 2));

        assertFalse(result.success());
        assertEquals("restaurant_closed", result.message());
    }

    @Test
    void shouldRejectReservationsForTemporaryClosure() {
        LocalDate wednesday = nextDateFor(DayOfWeek.WEDNESDAY);
        createTemporaryClosure(wednesday);

        ReservationRepositoryPort.ReservationResult result =
                repository.createReservation(request(wednesday, LocalTime.of(19, 0), 2));

        assertFalse(result.success());
        assertEquals("restaurant_closed", result.message());
    }

    @Test
    void shouldRejectReservationsBeyondAdvanceWindow() {
        LocalDate date = LocalDate.now(ZoneId.systemDefault()).plusDays(61);

        ReservationRepositoryPort.ReservationResult result =
                repository.createReservation(request(date, LocalTime.of(19, 0), 2));

        assertFalse(result.success());
        assertEquals("too_far_in_advance", result.message());
    }

    @Test
    void shouldRejectReservationsWhenCapacityIsInsufficient() {
        ReservationSlot slot = nextReservableSlot();

        ReservationRepositoryPort.ReservationResult result =
                repository.createReservation(request(slot.date(), slot.time(), 30));

        assertFalse(result.success());
        assertEquals("no_table_available", result.message());
    }

    private ReservationSlot nextReservableSlot() {
        for (int offset = 0; offset < 21; offset++) {
            LocalDate date = LocalDate.now(ZoneId.systemDefault()).plusDays(offset);
            if (isTemporarilyClosed(date)) {
                continue;
            }
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.THURSDAY) {
                return new ReservationSlot(date, LocalTime.of(19, 0));
            }
            if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                return new ReservationSlot(date, LocalTime.of(12, 0));
            }
            if (dayOfWeek == DayOfWeek.SUNDAY) {
                return new ReservationSlot(date, LocalTime.of(11, 30));
            }
        }
        throw new IllegalStateException("Unable to find a reservable slot");
    }

    private boolean isTemporarilyClosed(LocalDate date) {
        return dsl.fetchExists(
                dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE,
                dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(ESTABLISHMENT_ID)
                        .and(dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE.CLOSURE_DATE.eq(date))
                        .and(dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE.ACTIVE.isTrue()));
    }

    private void occupyAllTables(LocalDate date, LocalTime time) {
        int index = 0;
        List<UUID> tableIds = dsl.select(RESTAURANT_TABLE.ID)
                .from(RESTAURANT_TABLE)
                .where(RESTAURANT_TABLE.ESTABLISHMENT_ID.eq(ESTABLISHMENT_ID))
                .and(RESTAURANT_TABLE.ACTIVE.isTrue())
                .orderBy(RESTAURANT_TABLE.TABLE_NUMBER.asc())
                .fetch(record -> record.get(RESTAURANT_TABLE.ID));

        for (UUID tableId : tableIds) {
            index++;
            UUID reservationId = UUID.fromString(String.format("d2000000-0000-0000-0000-%012d", index));
            dsl.insertInto(RESTAURANT_RESERVATION)
                    .set(RESTAURANT_RESERVATION.ID, reservationId)
                    .set(RESTAURANT_RESERVATION.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                    .set(RESTAURANT_RESERVATION.REFERENCE_NUMBER, String.format("OCC%05d", index))
                    .set(RESTAURANT_RESERVATION.CHANNEL_USER_ID, "occupier-" + index)
                    .set(RESTAURANT_RESERVATION.RESERVATION_NAME, "Occupied " + index)
                    .set(RESTAURANT_RESERVATION.DATE, date)
                    .set(RESTAURANT_RESERVATION.TIME, time)
                    .set(RESTAURANT_RESERVATION.DURATION_MINUTES, 120)
                    .set(RESTAURANT_RESERVATION.PEOPLE_COUNT, 2)
                    .set(RESTAURANT_RESERVATION.STATUS, "CONFIRMED")
                    .execute();

            dsl.insertInto(RESTAURANT_RESERVATION_TABLE)
                    .set(RESTAURANT_RESERVATION_TABLE.RESERVATION_ID, reservationId)
                    .set(RESTAURANT_RESERVATION_TABLE.TABLE_ID, tableId)
                    .execute();
        }
    }

    private record ReservationSlot(LocalDate date, LocalTime time) {
    }
}
