package dev.stephyu.conversation.application;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ReservationCapacityService {

    public ReservationCapacityDecision evaluate(ReservationCapacityContext context) {
        Objects.requireNonNull(context, "context must not be null");

        ReservationConfig config = Objects.requireNonNull(context.config(), "config must not be null");
        LocalDate date = Objects.requireNonNull(context.date(), "date must not be null");
        LocalTime time = Objects.requireNonNull(context.time(), "time must not be null");
        int peopleCount = context.peopleCount();

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

        if (context.closure() != null) {
            return failure(
                    "closed_exception",
                    peopleCount,
                    config.reservationDurationMinutes(),
                    0,
                    config.allowTableMerging(),
                    List.of("The restaurant is closed on this date."));
        }

        List<OpeningSlot> daySlots = context.daySlots();
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
                context.tables(),
                context.reservations(),
                date,
                time,
                config.reservationDurationMinutes());
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

    private static ReservationCapacityDecision success(
            int requestedPeopleCount,
            int reservationDurationMinutes,
            int availableSeats,
            List<TableCandidate> selectedTables,
            boolean canMergeTables) {
        List<String> selectedTableNumbers = selectedTables.stream()
                .map(TableCandidate::tableNumber)
                .toList();
        return new ReservationCapacityDecision(new ReservationRepositoryPort.ReservationCapacityResult(
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

    private static ReservationCapacityDecision failure(
            String reason,
            int requestedPeopleCount,
            int reservationDurationMinutes,
            int availableSeats,
            boolean canMergeTables,
            List<String> suggestedAlternatives) {
        return new ReservationCapacityDecision(new ReservationRepositoryPort.ReservationCapacityResult(
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

    private static List<TableCandidate> loadAvailableTables(
            List<TableCandidate> tables,
            List<ExistingReservation> reservations,
            LocalDate date,
            LocalTime time,
            int durationMinutes) {
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        Set<UUID> occupiedTableIds = loadOccupiedTableIds(reservations, date, start, end);

        return tables.stream()
                .filter(TableCandidate::active)
                .filter(table -> !occupiedTableIds.contains(table.tableId()))
                .sorted((left, right) -> {
                    int seatComparison = Integer.compare(left.seatCount(), right.seatCount());
                    if (seatComparison != 0) {
                        return seatComparison;
                    }
                    return left.tableNumber().compareTo(right.tableNumber());
                })
                .toList();
    }

    private static Set<UUID> loadOccupiedTableIds(
            List<ExistingReservation> reservations,
            LocalDate date,
            LocalDateTime start,
            LocalDateTime end) {
        Set<UUID> occupiedTableIds = new HashSet<>();
        for (ExistingReservation reservation : reservations) {
            if (!reservation.date().equals(date)) {
                continue;
            }
            LocalDateTime existingStart = LocalDateTime.of(date, reservation.time());
            LocalDateTime existingEnd = existingStart.plusMinutes(reservation.durationMinutes());
            if (reservation.active() && existingStart.isBefore(end) && start.isBefore(existingEnd)) {
                occupiedTableIds.add(reservation.tableId());
            }
        }
        return occupiedTableIds;
    }

    private static List<TableCandidate> selectTables(List<TableCandidate> availableTables, int peopleCount, boolean allowMerging) {
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

    private static void searchCombinations(
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

    public record ReservationCapacityContext(
            UUID establishmentId,
            LocalDate date,
            LocalTime time,
            int peopleCount,
            ReservationConfig config,
            @Nullable ClosureRecord closure,
            List<OpeningSlot> daySlots,
            List<TableCandidate> tables,
            List<ExistingReservation> reservations) {
    }

    public record ReservationCapacityDecision(
            ReservationRepositoryPort.ReservationCapacityResult result,
            List<TableCandidate> selectedTables) {
    }

    public record ReservationConfig(int reservationDurationMinutes, boolean allowTableMerging, int maxAdvanceDays) {
    }

    public record ClosureRecord(LocalDate closureDate, String reason) {
    }

    public record OpeningSlot(LocalTime openTime, LocalTime closeTime) {
    }

    public record TableCandidate(UUID tableId, String tableNumber, int seatCount, boolean active) {
    }

    public record ExistingReservation(UUID tableId, LocalDate date, LocalTime time, int durationMinutes, boolean active) {
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
