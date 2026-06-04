package dev.stephyu.conversation.application.port.outbound;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface RestaurantAvailabilityRepositoryPort {

    AvailabilitySnapshot loadAvailability(String establishmentId, @Nullable LocalDate date, @Nullable LocalTime time);

    @NullMarked
    record AvailabilitySnapshot(
            String establishmentId,
            List<WeeklyOpeningHours> weeklyOpeningHours,
            List<ClosureSummary> upcomingClosures,
            @Nullable RequestedAvailability requestedAvailability) {
    }

    @NullMarked
    record WeeklyOpeningHours(int dayOfWeek, String dayName, List<OpeningSlot> slots) {
    }

    @NullMarked
    record OpeningSlot(LocalTime openTime, LocalTime closeTime) {
    }

    @NullMarked
    record ClosureSummary(LocalDate closureDate, @Nullable String reason) {
    }

    @NullMarked
    record RequestedAvailability(
            LocalDate date,
            @Nullable LocalTime time,
            boolean openForDate,
            @Nullable Boolean openForTime,
            String status,
            List<OpeningSlot> daySlots,
            @Nullable ClosureSummary closure,
            @Nullable String reason) {
    }
}
