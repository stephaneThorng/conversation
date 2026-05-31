package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_OPENING_HOURS;

import dev.stephyu.conversation.application.port.outbound.RestaurantAvailabilityRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PostgresRestaurantAvailabilityRepository implements RestaurantAvailabilityRepositoryPort {

    private final DSLContext dsl;

    public PostgresRestaurantAvailabilityRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    }

    @Override
    public AvailabilitySnapshot loadAvailability(String establishmentId, @Nullable LocalDate date, @Nullable LocalTime time) {
        UUID parsedEstablishmentId = UUID.fromString(requireText(establishmentId, "establishmentId"));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Map<Integer, List<OpeningSlot>> openingHoursByDay = loadWeeklyOpeningHours(parsedEstablishmentId);
        List<ClosureSummary> upcomingClosures = loadUpcomingClosures(parsedEstablishmentId, today);
        RequestedAvailability requestedAvailability = date == null
                ? null
                : buildRequestedAvailability(parsedEstablishmentId, date, time, openingHoursByDay);

        List<WeeklyOpeningHours> weeklyOpeningHours = new ArrayList<>(7);
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            weeklyOpeningHours.add(new WeeklyOpeningHours(
                    dayOfWeek,
                    dayName(dayOfWeek),
                    openingHoursByDay.getOrDefault(dayOfWeek, List.of())));
        }

        return new AvailabilitySnapshot(
                parsedEstablishmentId.toString(),
                List.copyOf(weeklyOpeningHours),
                List.copyOf(upcomingClosures),
                requestedAvailability);
    }

    private Map<Integer, List<OpeningSlot>> loadWeeklyOpeningHours(UUID establishmentId) {
        Map<Integer, List<OpeningSlot>> openingHoursByDay = new LinkedHashMap<>();
        dsl.select(
                        RESTAURANT_OPENING_HOURS.DAY_OF_WEEK,
                        RESTAURANT_OPENING_HOURS.OPEN_TIME,
                        RESTAURANT_OPENING_HOURS.CLOSE_TIME)
                .from(RESTAURANT_OPENING_HOURS)
                .where(RESTAURANT_OPENING_HOURS.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_OPENING_HOURS.ACTIVE.isTrue())
                .orderBy(RESTAURANT_OPENING_HOURS.DAY_OF_WEEK.asc(),
                        RESTAURANT_OPENING_HOURS.OPEN_TIME.asc(),
                        RESTAURANT_OPENING_HOURS.CLOSE_TIME.asc())
                .fetch()
                .forEach(record -> {
                    Short dayOfWeek = record.get(RESTAURANT_OPENING_HOURS.DAY_OF_WEEK);
                    openingHoursByDay.computeIfAbsent(dayOfWeek.intValue(), ignored -> new ArrayList<>()).add(
                            new OpeningSlot(
                                    record.get(RESTAURANT_OPENING_HOURS.OPEN_TIME),
                                    record.get(RESTAURANT_OPENING_HOURS.CLOSE_TIME)));
                });

        openingHoursByDay.replaceAll((key, value) -> List.copyOf(value));
        return Map.copyOf(openingHoursByDay);
    }

    private List<ClosureSummary> loadUpcomingClosures(UUID establishmentId, LocalDate today) {
        return dsl.select(
                        RESTAURANT_CLOSURE.CLOSURE_DATE,
                        RESTAURANT_CLOSURE.REASON)
                .from(RESTAURANT_CLOSURE)
                .where(RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_CLOSURE.ACTIVE.isTrue())
                .and(RESTAURANT_CLOSURE.CLOSURE_DATE.ge(today))
                .orderBy(RESTAURANT_CLOSURE.CLOSURE_DATE.asc())
                .fetch(record -> new ClosureSummary(
                        record.get(RESTAURANT_CLOSURE.CLOSURE_DATE),
                        record.get(RESTAURANT_CLOSURE.REASON)));
    }

    private RequestedAvailability buildRequestedAvailability(
            UUID establishmentId,
            LocalDate date,
            @Nullable LocalTime time,
            Map<Integer, List<OpeningSlot>> openingHoursByDay) {
        Optional<ClosureSummary> closure = dsl.select(
                        RESTAURANT_CLOSURE.CLOSURE_DATE,
                        RESTAURANT_CLOSURE.REASON)
                .from(RESTAURANT_CLOSURE)
                .where(RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(establishmentId))
                .and(RESTAURANT_CLOSURE.ACTIVE.isTrue())
                .and(RESTAURANT_CLOSURE.CLOSURE_DATE.eq(date))
                .fetchOptional(record -> new ClosureSummary(
                        record.get(RESTAURANT_CLOSURE.CLOSURE_DATE),
                        record.get(RESTAURANT_CLOSURE.REASON)));

        List<OpeningSlot> daySlots = openingHoursByDay.getOrDefault(date.getDayOfWeek().getValue(), List.of());
        boolean openForDate = closure.isEmpty() && !daySlots.isEmpty();
        Boolean openForTime = null;
        String status;
        String reason = null;

        if (closure.isPresent()) {
            status = "CLOSED";
            reason = "closure";
            openForDate = false;
            if (time != null) {
                openForTime = false;
            }
        } else if (daySlots.isEmpty()) {
            status = "CLOSED";
            reason = "no_opening_hours";
            if (time != null) {
                openForTime = false;
            }
        } else {
            status = "OPEN";
            if (time != null) {
                openForTime = daySlots.stream().anyMatch(slot ->
                        !time.isBefore(slot.openTime()) && time.isBefore(slot.closeTime()));
                if (!openForTime) {
                    status = "CLOSED";
                    reason = "outside_opening_hours";
                }
            }
        }

        return new RequestedAvailability(
                date,
                time,
                openForDate,
                openForTime,
                status,
                daySlots,
                closure.orElse(null),
                reason);
    }

    private static String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "MONDAY";
            case 2 -> "TUESDAY";
            case 3 -> "WEDNESDAY";
            case 4 -> "THURSDAY";
            case 5 -> "FRIDAY";
            case 6 -> "SATURDAY";
            case 7 -> "SUNDAY";
            default -> throw new IllegalArgumentException("Invalid dayOfWeek: " + dayOfWeek);
        };
    }

    private static String requireText(String value, String fieldName) {
        String trimmed = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
