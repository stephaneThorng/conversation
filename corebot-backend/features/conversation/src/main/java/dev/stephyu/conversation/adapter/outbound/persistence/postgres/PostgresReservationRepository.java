package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_OPENING_HOURS;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_CONFIG;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_TABLE_MAP;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_TABLE;

import dev.stephyu.conversation.application.ReservationCapacityService;
import dev.stephyu.conversation.application.ReservationCapacityService.ClosureRecord;
import dev.stephyu.conversation.application.ReservationCapacityService.ExistingReservation;
import dev.stephyu.conversation.application.ReservationCapacityService.OpeningSlot;
import dev.stephyu.conversation.application.ReservationCapacityService.ReservationCapacityContext;
import dev.stephyu.conversation.application.ReservationCapacityService.ReservationCapacityDecision;
import dev.stephyu.conversation.application.ReservationCapacityService.ReservationConfig;
import dev.stephyu.conversation.application.ReservationCapacityService.TableCandidate;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PostgresReservationRepository implements ReservationRepositoryPort {

    private final DSLContext dsl;
    private final ReservationCapacityService capacityService;

    public PostgresReservationRepository(DSLContext dsl) {
        this(dsl, new ReservationCapacityService());
    }

    public PostgresReservationRepository(DSLContext dsl, ReservationCapacityService capacityService) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
        this.capacityService = Objects.requireNonNull(capacityService, "capacityService must not be null");
    }

    @Override
    public ReservationResult createReservation(CreateReservationRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        try {
            UUID establishmentId = parseUuid(request.establishmentId());
            String channelUserId = normalizeRequired(request.channelUserId(), "channelUserId");
            String reservationName = normalizeRequired(request.reservationName(), "reservationName");
            LocalDate date = Objects.requireNonNull(request.date(), "date must not be null");
            LocalTime time = Objects.requireNonNull(request.time(), "time must not be null");

            return dsl.transactionResult(configuration -> {
                DSLContext tx = DSL.using(configuration);
                ReservationCapacityDecision decision = evaluateCapacity(tx, establishmentId, date, time, request.peopleCount());
                if (!decision.result().canReserve()) {
                    return ReservationResult.failure(decision.result().reason());
                }

                String referenceNumber = generateUniqueReference(tx);
                UUID reservationId = UUID.randomUUID();
                tx.insertInto(RESTAURANT_RESERVATION)
                        .set(RESTAURANT_RESERVATION.ID, reservationId)
                        .set(RESTAURANT_RESERVATION.ESTABLISHMENT_ID, establishmentId)
                        .set(RESTAURANT_RESERVATION.REFERENCE_NUMBER, referenceNumber)
                        .set(RESTAURANT_RESERVATION.CHANNEL_USER_ID, channelUserId)
                        .set(RESTAURANT_RESERVATION.RESERVATION_NAME, reservationName)
                        .set(RESTAURANT_RESERVATION.DATE, date)
                        .set(RESTAURANT_RESERVATION.TIME, time)
                        .set(RESTAURANT_RESERVATION.DURATION_MINUTES, decision.result().reservationDurationMinutes())
                        .set(RESTAURANT_RESERVATION.PEOPLE_COUNT, request.peopleCount())
                        .set(RESTAURANT_RESERVATION.STATUS, "PENDING")
                        .execute();

                for (TableCandidate table : decision.selectedTables()) {
                    tx.insertInto(RESTAURANT_RESERVATION_TABLE_MAP)
                            .set(RESTAURANT_RESERVATION_TABLE_MAP.RESERVATION_ID, reservationId)
                            .set(RESTAURANT_RESERVATION_TABLE_MAP.TABLE_ID, table.tableId())
                            .execute();
                }
                return ReservationResult.success(referenceNumber);
            });
        } catch (IllegalArgumentException exception) {
            return ReservationResult.failure("invalid_reservation_request");
        }
    }

    @Override
    public ReservationCapacityResult checkReservationCapacity(ReservationCapacityRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        try {
            UUID establishmentId = parseUuid(request.establishmentId());
            LocalDate date = Objects.requireNonNull(request.date(), "date must not be null");
            LocalTime time = Objects.requireNonNull(request.time(), "time must not be null");
            return evaluateCapacity(dsl, establishmentId, date, time, request.peopleCount()).result();
        } catch (IllegalArgumentException exception) {
            return new ReservationCapacityResult(
                    false,
                    "invalid_reservation_request",
                    request.peopleCount(),
                    0,
                    0,
                    0,
                    false,
                    List.of(),
                    List.of());
        }
    }

    @Override
    public Optional<ReservationSummary> findReservation(
            String establishmentId, String referenceNumber, String channelUserId) {
        try {
            UUID parsedEstablishmentId = parseUuid(establishmentId);
            String normalizedReference = normalizeReference(referenceNumber);
            String normalizedChannelUserId = normalizeRequired(channelUserId, "channelUserId");

            return dsl.select(
                            RESTAURANT_RESERVATION.REFERENCE_NUMBER,
                            RESTAURANT_RESERVATION.RESERVATION_NAME,
                            RESTAURANT_RESERVATION.DATE,
                            RESTAURANT_RESERVATION.TIME,
                            RESTAURANT_RESERVATION.PEOPLE_COUNT)
                    .from(RESTAURANT_RESERVATION)
                    .where(RESTAURANT_RESERVATION.ESTABLISHMENT_ID.eq(parsedEstablishmentId))
                    .and(RESTAURANT_RESERVATION.REFERENCE_NUMBER.eq(normalizedReference))
                    .and(RESTAURANT_RESERVATION.CHANNEL_USER_ID.eq(normalizedChannelUserId))
                    .and(RESTAURANT_RESERVATION.STATUS.ne("CANCELLED"))
                    .fetchOptional(record -> new ReservationSummary(
                            record.get(RESTAURANT_RESERVATION.REFERENCE_NUMBER),
                            record.get(RESTAURANT_RESERVATION.RESERVATION_NAME),
                            record.get(RESTAURANT_RESERVATION.DATE),
                            record.get(RESTAURANT_RESERVATION.TIME),
                            record.get(RESTAURANT_RESERVATION.PEOPLE_COUNT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public ReservationResult cancelReservation(String establishmentId, String referenceNumber, String channelUserId) {
        try {
            UUID parsedEstablishmentId = parseUuid(establishmentId);
            String normalizedReference = normalizeReference(referenceNumber);
            String normalizedChannelUserId = normalizeRequired(channelUserId, "channelUserId");

            int updatedRows = dsl.update(RESTAURANT_RESERVATION)
                    .set(RESTAURANT_RESERVATION.STATUS, "CANCELLED")
                    .set(RESTAURANT_RESERVATION.UPDATED_AT, DSL.currentOffsetDateTime())
                    .where(RESTAURANT_RESERVATION.ESTABLISHMENT_ID.eq(parsedEstablishmentId))
                    .and(RESTAURANT_RESERVATION.REFERENCE_NUMBER.eq(normalizedReference))
                    .and(RESTAURANT_RESERVATION.CHANNEL_USER_ID.eq(normalizedChannelUserId))
                    .and(RESTAURANT_RESERVATION.STATUS.ne("CANCELLED"))
                    .execute();
            if (updatedRows == 0) {
                return ReservationResult.failure("not_found");
            }
            return ReservationResult.success(normalizedReference);
        } catch (IllegalArgumentException exception) {
            return ReservationResult.failure("invalid_reservation_request");
        }
    }

    private ReservationCapacityDecision evaluateCapacity(
            DSLContext tx, UUID establishmentId, LocalDate date, LocalTime time, int peopleCount) {
        ReservationConfig config = loadReservationConfig(tx, establishmentId).orElse(null);
        if (config == null) {
            return new ReservationCapacityDecision(
                    new ReservationRepositoryPort.ReservationCapacityResult(
                            false,
                            "reservation_config_missing",
                            peopleCount,
                            0,
                            0,
                            0,
                            false,
                            List.of(),
                            List.of()),
                    List.of());
        }

        Optional<ClosureRecord> closure = loadClosure(tx, establishmentId, date);
        List<OpeningSlot> daySlots = loadOpeningSlots(tx, establishmentId, date.getDayOfWeek().getValue());
        List<TableCandidate> tables = loadTables(tx, establishmentId);
        List<ExistingReservation> reservations = loadActiveReservations(tx, establishmentId, date);

        return capacityService.evaluate(new ReservationCapacityContext(
                establishmentId,
                date,
                time,
                peopleCount,
                config,
                closure.orElse(null),
                daySlots,
                tables,
                reservations));
    }

    private Optional<ReservationConfig> loadReservationConfig(DSLContext tx, UUID establishmentId) {
        return tx.select(
                        RESTAURANT_RESERVATION_CONFIG.RESERVATION_DURATION_MINUTES,
                        RESTAURANT_RESERVATION_CONFIG.ALLOW_TABLE_MERGING,
                        RESTAURANT_RESERVATION_CONFIG.MAX_ADVANCE_DAYS)
                .from(RESTAURANT_RESERVATION_CONFIG)
                .where(RESTAURANT_RESERVATION_CONFIG.ESTABLISHMENT_ID.eq(establishmentId))
                .fetchOptional(record -> new ReservationConfig(
                        record.get(RESTAURANT_RESERVATION_CONFIG.RESERVATION_DURATION_MINUTES),
                        record.get(RESTAURANT_RESERVATION_CONFIG.ALLOW_TABLE_MERGING),
                        record.get(RESTAURANT_RESERVATION_CONFIG.MAX_ADVANCE_DAYS)));
    }

    private Optional<ClosureRecord> loadClosure(DSLContext tx, UUID establishmentId, LocalDate date) {
        return tx.select(
                        RESTAURANT_CLOSURE.CLOSURE_DATE,
                        RESTAURANT_CLOSURE.REASON)
                .from(RESTAURANT_CLOSURE)
                .where(RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_CLOSURE.CLOSURE_DATE.eq(date))
                .and(RESTAURANT_CLOSURE.ACTIVE.isTrue())
                .fetchOptional(record -> new ClosureRecord(
                        record.get(RESTAURANT_CLOSURE.CLOSURE_DATE),
                        record.get(RESTAURANT_CLOSURE.REASON)));
    }

    private List<OpeningSlot> loadOpeningSlots(DSLContext tx, UUID establishmentId, int dayOfWeek) {
        return tx.select(
                        RESTAURANT_OPENING_HOURS.OPEN_TIME,
                        RESTAURANT_OPENING_HOURS.CLOSE_TIME)
                .from(RESTAURANT_OPENING_HOURS)
                .where(RESTAURANT_OPENING_HOURS.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_OPENING_HOURS.DAY_OF_WEEK.eq((short) dayOfWeek))
                .and(RESTAURANT_OPENING_HOURS.ACTIVE.isTrue())
                .orderBy(RESTAURANT_OPENING_HOURS.OPEN_TIME.asc(),
                        RESTAURANT_OPENING_HOURS.CLOSE_TIME.asc())
                .fetch(record -> new OpeningSlot(
                        record.get(RESTAURANT_OPENING_HOURS.OPEN_TIME),
                        record.get(RESTAURANT_OPENING_HOURS.CLOSE_TIME)));
    }

    private List<TableCandidate> loadTables(DSLContext tx, UUID establishmentId) {
        return tx.select(
                        RESTAURANT_TABLE.ID,
                        RESTAURANT_TABLE.TABLE_NUMBER,
                        RESTAURANT_TABLE.SEAT_COUNT,
                        RESTAURANT_TABLE.ACTIVE)
                .from(RESTAURANT_TABLE)
                .where(RESTAURANT_TABLE.ESTABLISHMENT_ID.eq(establishmentId))
                .fetch(record -> new TableCandidate(
                        record.get(RESTAURANT_TABLE.ID),
                        record.get(RESTAURANT_TABLE.TABLE_NUMBER),
                        record.get(RESTAURANT_TABLE.SEAT_COUNT),
                        Boolean.TRUE.equals(record.get(RESTAURANT_TABLE.ACTIVE))));
    }

    private List<ExistingReservation> loadActiveReservations(DSLContext tx, UUID establishmentId, LocalDate date) {
        return tx.select(
                        RESTAURANT_RESERVATION_TABLE_MAP.TABLE_ID,
                        RESTAURANT_RESERVATION.DATE,
                        RESTAURANT_RESERVATION.TIME,
                        RESTAURANT_RESERVATION.DURATION_MINUTES,
                        RESTAURANT_RESERVATION.STATUS)
                .from(RESTAURANT_RESERVATION)
                .join(RESTAURANT_RESERVATION_TABLE_MAP)
                .on(RESTAURANT_RESERVATION_TABLE_MAP.RESERVATION_ID.eq(RESTAURANT_RESERVATION.ID))
                .where(RESTAURANT_RESERVATION.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_RESERVATION.DATE.eq(date))
                .and(RESTAURANT_RESERVATION.STATUS.in("PENDING", "CONFIRMED"))
                .fetch(record -> new ExistingReservation(
                        record.get(RESTAURANT_RESERVATION_TABLE_MAP.TABLE_ID),
                        record.get(RESTAURANT_RESERVATION.DATE),
                        record.get(RESTAURANT_RESERVATION.TIME),
                        record.get(RESTAURANT_RESERVATION.DURATION_MINUTES),
                        true));
    }

    private String generateUniqueReference(DSLContext tx) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            boolean exists = tx.fetchExists(
                    RESTAURANT_RESERVATION,
                    RESTAURANT_RESERVATION.REFERENCE_NUMBER.eq(candidate));
            if (!exists) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique reservation reference");
    }

    private static UUID parseUuid(String value) {
        return UUID.fromString(normalizeRequired(value, "establishmentId"));
    }

    private static String normalizeReference(String referenceNumber) {
        return normalizeRequired(referenceNumber, "referenceNumber").toUpperCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value, String fieldName) {
        String trimmed = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
