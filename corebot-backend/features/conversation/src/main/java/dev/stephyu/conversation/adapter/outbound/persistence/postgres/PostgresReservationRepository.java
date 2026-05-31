package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_OPENING_HOURS;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_CONFIG;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_TABLE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_TABLE;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PostgresReservationRepository implements ReservationRepositoryPort {

    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED");

    private final DSLContext dsl;

    public PostgresReservationRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
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
                CapacityEvaluation evaluation = evaluateCapacity(tx, establishmentId, date, time, request.peopleCount());
                if (!evaluation.result().canReserve()) {
                    return ReservationResult.failure(evaluation.result().reason());
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
                        .set(RESTAURANT_RESERVATION.DURATION_MINUTES, evaluation.result().reservationDurationMinutes())
                        .set(RESTAURANT_RESERVATION.PEOPLE_COUNT, request.peopleCount())
                        .set(RESTAURANT_RESERVATION.STATUS, "PENDING")
                        .execute();

                for (TableCandidate table : evaluation.selectedTables()) {
                    tx.insertInto(RESTAURANT_RESERVATION_TABLE)
                            .set(RESTAURANT_RESERVATION_TABLE.RESERVATION_ID, reservationId)
                            .set(RESTAURANT_RESERVATION_TABLE.TABLE_ID, table.tableId())
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

    private CapacityEvaluation evaluateCapacity(
            DSLContext tx, UUID establishmentId, LocalDate date, LocalTime time, int peopleCount) {
        ReservationConfig config = loadReservationConfig(tx, establishmentId).orElse(null);
        if (config == null) {
            return failure(
                    "reservation_config_missing",
                    peopleCount,
                    0,
                    0,
                    false,
                    List.of("Reservation configuration is missing for this establishment."));
        }
        if (peopleCount < 1) {
            return failure(
                    "invalid_people_count",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("Please request at least one guest."));
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (date.isBefore(today)) {
            return failure(
                    "date_in_past",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("Please choose a future date."));
        }
        if (date.isAfter(today.plusDays(config.maxAdvanceDays()))) {
            return failure(
                    "too_far_in_advance",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("Please choose a closer date."));
        }

        Optional<ClosureRecord> closure = loadClosure(tx, establishmentId, date);
        if (closure.isPresent()) {
            return failure(
                    "closed_exception",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("The restaurant is closed on this date."));
        }

        List<OpeningSlot> daySlots = loadOpeningSlots(tx, establishmentId, date.getDayOfWeek().getValue());
        if (daySlots.isEmpty()) {
            return failure(
                    "closed_day",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("The restaurant is closed on this day."));
        }

        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(config.reservationDurationMinutes());
        boolean withinHours = daySlots.stream().anyMatch(slot ->
                !time.isBefore(slot.openTime()) && !end.toLocalTime().isAfter(slot.closeTime()));
        if (!withinHours) {
            return failure(
                    "outside_opening_hours",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    formatSuggestions(daySlots));
        }

        List<TableCandidate> availableTables = loadAvailableTables(
                tx, establishmentId, date, time, config.reservationDurationMinutes());
        List<TableCandidate> selectedTables = selectTables(availableTables, peopleCount, config.allowTableMerging());
        if (selectedTables.isEmpty()) {
            int availableSeats = availableTables.stream().mapToInt(TableCandidate::seatCount).sum();
            String reason = config.allowTableMerging() ? "no_capacity"
                    : (availableSeats >= peopleCount ? "needs_merging" : "no_capacity");
            List<String> alternatives = new ArrayList<>();
            alternatives.add("Try another time within the opening hours.");
            if (!config.allowTableMerging() && availableSeats >= peopleCount) {
                alternatives.add("This group fits only if table merging is enabled.");
            }
            return failure(
                    reason,
                    peopleCount,
                    config.reservationDurationMinutes(),
                    availableSeats,
                    config.allowTableMerging(),
                    alternatives);
        }

        int availableSeats = selectedTables.stream().mapToInt(TableCandidate::seatCount).sum();
        return success(
                peopleCount,
                config.reservationDurationMinutes(),
                availableSeats,
                selectedTables,
                config.allowTableMerging());
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
                .orderBy(RESTAURANT_OPENING_HOURS.OPEN_TIME.asc(), RESTAURANT_OPENING_HOURS.CLOSE_TIME.asc())
                .fetch(record -> new OpeningSlot(
                        record.get(RESTAURANT_OPENING_HOURS.OPEN_TIME),
                        record.get(RESTAURANT_OPENING_HOURS.CLOSE_TIME)));
    }

    private List<TableCandidate> loadAvailableTables(
            DSLContext tx, UUID establishmentId, LocalDate date, LocalTime time, int durationMinutes) {
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        Set<UUID> occupiedTableIds = loadOccupiedTableIds(tx, establishmentId, date, start, end);

        return tx.select(
                        RESTAURANT_TABLE.ID,
                        RESTAURANT_TABLE.TABLE_NUMBER,
                        RESTAURANT_TABLE.SEAT_COUNT)
                .from(RESTAURANT_TABLE)
                .where(RESTAURANT_TABLE.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_TABLE.ACTIVE.isTrue())
                .orderBy(RESTAURANT_TABLE.SEAT_COUNT.asc(), RESTAURANT_TABLE.TABLE_NUMBER.asc())
                .fetch(record -> new TableCandidate(
                        record.get(RESTAURANT_TABLE.ID),
                        record.get(RESTAURANT_TABLE.TABLE_NUMBER),
                        record.get(RESTAURANT_TABLE.SEAT_COUNT)))
                .stream()
                .filter(table -> !occupiedTableIds.contains(table.tableId()))
                .toList();
    }

    private Set<UUID> loadOccupiedTableIds(
            DSLContext tx, UUID establishmentId, LocalDate date, LocalDateTime start, LocalDateTime end) {
        Set<UUID> occupiedTableIds = new HashSet<>();
        List<Record4<UUID, LocalTime, Integer, String>> reservations = tx.select(
                        RESTAURANT_RESERVATION_TABLE.TABLE_ID,
                        RESTAURANT_RESERVATION.TIME,
                        RESTAURANT_RESERVATION.DURATION_MINUTES,
                        RESTAURANT_RESERVATION.STATUS)
                .from(RESTAURANT_RESERVATION)
                .join(RESTAURANT_RESERVATION_TABLE)
                .on(RESTAURANT_RESERVATION_TABLE.RESERVATION_ID.eq(RESTAURANT_RESERVATION.ID))
                .where(RESTAURANT_RESERVATION.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_RESERVATION.DATE.eq(date))
                .and(RESTAURANT_RESERVATION.STATUS.in(ACTIVE_STATUSES))
                .fetch();

        for (Record4<UUID, LocalTime, Integer, String> reservation : reservations) {
            LocalDateTime existingStart = LocalDateTime.of(date, reservation.value2());
            LocalDateTime existingEnd = existingStart.plusMinutes(reservation.value3());
            if (existingStart.isBefore(end) && start.isBefore(existingEnd)) {
                occupiedTableIds.add(reservation.value1());
            }
        }
        return occupiedTableIds;
    }

    private List<TableCandidate> selectTables(List<TableCandidate> availableTables, int peopleCount, boolean allowMerging) {
        if (availableTables.isEmpty()) {
            return List.of();
        }
        if (!allowMerging) {
            return availableTables.stream()
                    .filter(table -> table.seatCount() >= peopleCount)
                    .findFirst()
                    .map(List::of)
                    .orElse(List.of());
        }

        BestCombination bestCombination = new BestCombination();
        searchCombinations(availableTables, 0, new ArrayList<>(), 0, peopleCount, bestCombination);
        return bestCombination.tables().orElse(List.of());
    }

    private void searchCombinations(
            List<TableCandidate> availableTables,
            int startIndex,
            List<TableCandidate> currentSelection,
            int currentSeats,
            int targetSeats,
            BestCombination bestCombination) {
        if (currentSeats >= targetSeats) {
            bestCombination.consider(currentSelection, currentSeats);
            return;
        }
        if (currentSelection.size() == 3) {
            return;
        }

        for (int index = startIndex; index < availableTables.size(); index++) {
            TableCandidate candidate = availableTables.get(index);
            currentSelection.add(candidate);
            searchCombinations(
                    availableTables,
                    index + 1,
                    currentSelection,
                    currentSeats + candidate.seatCount(),
                    targetSeats,
                    bestCombination);
            currentSelection.remove(currentSelection.size() - 1);
        }
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

    private CapacityEvaluation success(
            int requestedPeopleCount,
            int reservationDurationMinutes,
            int availableSeats,
            List<TableCandidate> selectedTables,
            boolean canMergeTables) {
        List<String> selectedTableNumbers = selectedTables.stream()
                .map(TableCandidate::tableNumber)
                .toList();
        return new CapacityEvaluation(new ReservationCapacityResult(
                true,
                "ok",
                requestedPeopleCount,
                reservationDurationMinutes,
                availableSeats,
                selectedTables.size(),
                canMergeTables,
                selectedTableNumbers,
                List.of()), selectedTables);
    }

    private CapacityEvaluation failure(
            String reason,
            int requestedPeopleCount,
            int reservationDurationMinutes,
            int availableSeats,
            boolean canMergeTables,
            List<String> suggestedAlternatives) {
        return new CapacityEvaluation(new ReservationCapacityResult(
                false,
                reason,
                requestedPeopleCount,
                reservationDurationMinutes,
                availableSeats,
                0,
                canMergeTables,
                List.of(),
                List.copyOf(suggestedAlternatives)), List.of());
    }

    private static List<String> formatSuggestions(List<OpeningSlot> daySlots) {
        if (daySlots.isEmpty()) {
            return List.of("No opening hours are configured for this day.");
        }
        List<String> suggestions = new ArrayList<>();
        for (OpeningSlot slot : daySlots) {
            suggestions.add(slot.openTime() + "-" + slot.closeTime());
        }
        return List.copyOf(suggestions);
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

    private record ReservationConfig(int reservationDurationMinutes, boolean allowTableMerging, int maxAdvanceDays) {
    }

    private record ClosureRecord(LocalDate closureDate, String reason) {
    }

    private record OpeningSlot(LocalTime openTime, LocalTime closeTime) {
    }

    private record TableCandidate(UUID tableId, String tableNumber, int seatCount) {
    }

    private record CapacityEvaluation(ReservationCapacityResult result, List<TableCandidate> selectedTables) {
    }

    private static final class BestCombination {
        private List<TableCandidate> tables = List.of();
        private int totalSeats = Integer.MAX_VALUE;

        private void consider(List<TableCandidate> candidateTables, int seats) {
            List<TableCandidate> snapshot = List.copyOf(candidateTables);
            if (snapshot.isEmpty()) {
                return;
            }
            if (tables.isEmpty()
                    || seats < totalSeats
                    || (seats == totalSeats && compareTables(snapshot, tables) < 0)) {
                tables = snapshot;
                totalSeats = seats;
            }
        }

        private Optional<List<TableCandidate>> tables() {
            return tables.isEmpty() ? Optional.empty() : Optional.of(tables);
        }

        private static int compareTables(List<TableCandidate> left, List<TableCandidate> right) {
            int sizeComparison = Integer.compare(left.size(), right.size());
            if (sizeComparison != 0) {
                return sizeComparison;
            }
            List<String> leftNumbers = left.stream().map(TableCandidate::tableNumber).sorted().toList();
            List<String> rightNumbers = right.stream().map(TableCandidate::tableNumber).sorted().toList();
            for (int index = 0; index < Math.min(leftNumbers.size(), rightNumbers.size()); index++) {
                int comparison = leftNumbers.get(index).compareTo(rightNumbers.get(index));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return Integer.compare(leftNumbers.size(), rightNumbers.size());
        }
    }
}
