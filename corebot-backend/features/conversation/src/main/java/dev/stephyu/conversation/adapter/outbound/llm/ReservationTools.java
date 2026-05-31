package dev.stephyu.conversation.adapter.outbound.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM-callable tools for reservation side effects.
 * establishmentId and channelUserId are passed explicitly by the LLM on every call
 * so a single instance serves all establishments and users.
 * channelUserId ensures a user can only access their own reservations.
 */
@NullMarked
public final class ReservationTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationTools.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ReservationRepositoryPort reservationRepository;

    public ReservationTools(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository must not be null");
    }

    @Tool("Creates a table reservation. Call only after the user has confirmed all details. " +
            "Returns the reservation reference number on success, or an error message on failure.")
    public String createReservation(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Channel user identifier from the context (e.g. phone number or platform user ID)") String channelUserId,
            @P("Customer name for the reservation") String customerName,
            @P("Reservation date in ISO format: YYYY-MM-DD") String date,
            @P("Reservation time in ISO format: HH:mm") String time,
            @P("Number of people (positive integer)") int peopleCount) {
        LOGGER.info("Tool createReservation: establishmentId={}, channelUserId={}, customerName={}, date={}, time={}, peopleCount={}",
                establishmentId, channelUserId, customerName, date, time, peopleCount);
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            LocalTime parsedTime = LocalTime.parse(time);
            var request = new ReservationRepositoryPort.CreateReservationRequest(
                    establishmentId, channelUserId, customerName.trim(), parsedDate, parsedTime, peopleCount);
            ReservationRepositoryPort.ReservationResult result = reservationRepository.createReservation(request);
            if (result.success()) {
                LOGGER.info("Tool createReservation: success, establishmentId={}, channelUserId={}, reference={}",
                        establishmentId, channelUserId, result.referenceNumber());
                return "Reservation created successfully. Reference number: " + result.referenceNumber();
            }
            LOGGER.warn("Tool createReservation: failure, establishmentId={}, channelUserId={}, message={}",
                    establishmentId, channelUserId, result.message());
            return "Failed to create reservation: " + result.message();
        } catch (DateTimeParseException e) {
            LOGGER.warn("Tool createReservation: invalid date/time format: date={}, time={}", date, time);
            return "Invalid date or time format. Please use YYYY-MM-DD for date and HH:mm for time.";
        } catch (Exception e) {
            LOGGER.error("Tool createReservation: unexpected error", e);
            return "An unexpected error occurred while creating the reservation. Please try again.";
        }
    }

    @Tool("Checks whether a reservation can be created before confirmation. " +
            "Use after opening hours have been verified and before calling createReservation. " +
            "Returns structured JSON with canReserve, reason, availableSeats, and suggested alternatives.")
    public String checkReservationCapacity(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Reservation date in ISO format: YYYY-MM-DD") String date,
            @P("Reservation time in ISO format: HH:mm") String time,
            @P("Number of people (positive integer)") int peopleCount) {
        LOGGER.info("Tool checkReservationCapacity: establishmentId={}, date={}, time={}, peopleCount={}",
                establishmentId, date, time, peopleCount);
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            LocalTime parsedTime = LocalTime.parse(time);
            ReservationRepositoryPort.ReservationCapacityResult result = reservationRepository.checkReservationCapacity(
                    new ReservationRepositoryPort.ReservationCapacityRequest(establishmentId, parsedDate, parsedTime, peopleCount));
            String payload = OBJECT_MAPPER.writeValueAsString(result);
            LOGGER.info("Tool checkReservationCapacity: success, establishmentId={}, payload_size={}",
                    establishmentId, payload.length());
            return payload;
        } catch (DateTimeParseException e) {
            LOGGER.warn("Tool checkReservationCapacity: invalid date/time format: date={}, time={}", date, time);
            return serializeFailure(peopleCount, "invalid_date_or_time");
        } catch (JsonProcessingException e) {
            LOGGER.error("Tool checkReservationCapacity: failed to serialize capacity result", e);
            return serializeFailure(peopleCount, "serialization_error");
        } catch (Exception e) {
            LOGGER.error("Tool checkReservationCapacity: unexpected error", e);
            return serializeFailure(peopleCount, "unexpected_error");
        }
    }

    @Tool("Checks the status of an existing reservation by its reference number. " +
            "Returns reservation details on success, or an error message if not found.")
    public String checkReservation(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Channel user identifier from the context (e.g. phone number or platform user ID)") String channelUserId,
            @P("Reservation reference number (e.g. A1B2C3D4)") String referenceNumber) {
        LOGGER.info("Tool checkReservation: establishmentId={}, channelUserId={}, referenceNumber={}",
                establishmentId, channelUserId, referenceNumber);
        try {
            Optional<ReservationRepositoryPort.ReservationSummary> found =
                    reservationRepository.findReservation(establishmentId, referenceNumber.trim().toUpperCase(Locale.ROOT), channelUserId);
            if (found.isEmpty()) {
                return "No reservation found with reference number: " + referenceNumber;
            }
            ReservationRepositoryPort.ReservationSummary summary = found.get();
            return "Reservation found: reference=" + summary.referenceNumber()
                    + ", name=" + summary.reservationName()
                    + ", date=" + summary.date()
                    + ", time=" + summary.time()
                    + ", people=" + summary.peopleCount();
        } catch (Exception e) {
            LOGGER.error("Tool checkReservation: unexpected error", e);
            return "An unexpected error occurred while looking up the reservation. Please try again.";
        }
    }

    @Tool("Cancels an existing reservation by its reference number. " +
            "Call only after the user has confirmed the cancellation. " +
            "Returns a success message or an error if the reservation was not found.")
    public String cancelReservation(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Channel user identifier from the context (e.g. phone number or platform user ID)") String channelUserId,
            @P("Reservation reference number to cancel (e.g. A1B2C3D4)") String referenceNumber) {
        LOGGER.info("Tool cancelReservation: establishmentId={}, channelUserId={}, referenceNumber={}",
                establishmentId, channelUserId, referenceNumber);
        try {
            ReservationRepositoryPort.ReservationResult result =
                    reservationRepository.cancelReservation(establishmentId, referenceNumber.trim().toUpperCase(Locale.ROOT), channelUserId);
            if (result.success()) {
                LOGGER.info("Tool cancelReservation: success, establishmentId={}, channelUserId={}, reference={}",
                        establishmentId, channelUserId, result.referenceNumber());
                return "Reservation " + result.referenceNumber() + " has been successfully cancelled.";
            }
            LOGGER.warn("Tool cancelReservation: failure, establishmentId={}, channelUserId={}, message={}",
                    establishmentId, channelUserId, result.message());
            return "Failed to cancel reservation: " + result.message();
        } catch (Exception e) {
            LOGGER.error("Tool cancelReservation: unexpected error", e);
            return "An unexpected error occurred while cancelling the reservation. Please try again.";
        }
    }

    private static String serializeFailure(int peopleCount, String reason) {
        try {
            return OBJECT_MAPPER.writeValueAsString(new ReservationRepositoryPort.ReservationCapacityResult(
                    false,
                    reason,
                    peopleCount,
                    0,
                    0,
                    0,
                    false,
                    List.of(),
                    List.of()));
        } catch (JsonProcessingException exception) {
            return "{\"canReserve\":false,\"reason\":\"" + reason + "\"}";
        }
    }
}
