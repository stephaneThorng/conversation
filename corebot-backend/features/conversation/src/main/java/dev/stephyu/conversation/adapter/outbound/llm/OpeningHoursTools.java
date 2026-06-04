package dev.stephyu.conversation.adapter.outbound.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.stephyu.conversation.application.port.outbound.RestaurantAvailabilityRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class OpeningHoursTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpeningHoursTools.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final RestaurantAvailabilityRepositoryPort repository;

    public OpeningHoursTools(RestaurantAvailabilityRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Tool("Returns the opening hours and exceptional closures for the current establishment. " +
            "Use when the user asks about opening hours, closures, availability, or whether a specific date/time is open. " +
            "Pass blank date and time if you only need the general weekly schedule.")
    public String getOpeningHours(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Requested date in ISO format YYYY-MM-DD, or blank if not checking a specific date") String date,
            @P("Requested time in ISO format HH:mm, or blank if not checking a specific time") String time) {
        LOGGER.info("Tool getOpeningHours: establishmentId={}, date={}, time={}", establishmentId, date, time);
        try {
            LocalDate parsedDate = parseDate(date);
            LocalTime parsedTime = parseTime(time);
            if (parsedTime != null && parsedDate == null) {
                return "Invalid opening hours request: a time can only be checked when a date is provided.";
            }

            RestaurantAvailabilityRepositoryPort.AvailabilitySnapshot snapshot =
                    repository.loadAvailability(establishmentId, parsedDate, parsedTime);
            String payload = OBJECT_MAPPER.writeValueAsString(snapshot);
            LOGGER.info("Tool getOpeningHours: success, establishmentId={}, payload_size={}", establishmentId, payload.length());
            return payload;
        } catch (DateTimeParseException exception) {
            LOGGER.warn("Tool getOpeningHours: invalid date/time format, date={}, time={}", date, time);
            return "Invalid date or time format. Please use YYYY-MM-DD for date and HH:mm for time.";
        } catch (JsonProcessingException exception) {
            LOGGER.error("Tool getOpeningHours: failed to serialize opening hours snapshot", exception);
            return "An unexpected error occurred while loading the opening hours information. Please try again.";
        } catch (Exception exception) {
            LOGGER.error("Tool getOpeningHours: unexpected error", exception);
            return "An unexpected error occurred while loading the opening hours information. Please try again.";
        }
    }

    private static @Nullable LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private static @Nullable LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalTime.parse(value.trim());
    }
}
