package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM-callable tools for reservation side effects.
 * establishmentId and sessionId are passed explicitly by the LLM on every call
 * so a single instance serves all establishments and sessions.
 */
@NullMarked
public final class ReservationTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationTools.class);

    private final ReservationRepositoryPort reservationRepository;

    public ReservationTools(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository must not be null");
    }

    @Tool("Creates a table reservation. Call only after the user has confirmed all details. " +
            "Returns the reservation reference number on success, or an error message on failure.")
    public String createReservation(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Session identifier of the user making the reservation, from the context") String sessionId,
            @P("Customer name for the reservation") String customerName,
            @P("Reservation date in ISO format: YYYY-MM-DD") String date,
            @P("Reservation time in ISO format: HH:mm") String time,
            @P("Number of people (positive integer)") int peopleCount) {
        LOGGER.info("Tool createReservation: establishmentId={}, sessionId={}, customerName={}, date={}, time={}, peopleCount={}",
                establishmentId, sessionId, customerName, date, time, peopleCount);
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            LocalTime parsedTime = LocalTime.parse(time);
            var request = new ReservationRepositoryPort.CreateReservationRequest(
                    customerName.trim(), parsedDate, parsedTime, peopleCount);
            ReservationRepositoryPort.ReservationResult result = reservationRepository.createReservation(request);
            if (result.success()) {
                LOGGER.info("Tool createReservation: success, establishmentId={}, sessionId={}, reference={}",
                        establishmentId, sessionId, result.referenceNumber());
                return "Reservation created successfully. Reference number: " + result.referenceNumber();
            }
            LOGGER.warn("Tool createReservation: failure, establishmentId={}, sessionId={}, message={}",
                    establishmentId, sessionId, result.message());
            return "Failed to create reservation: " + result.message();
        } catch (DateTimeParseException e) {
            LOGGER.warn("Tool createReservation: invalid date/time format: date={}, time={}", date, time);
            return "Invalid date or time format. Please use YYYY-MM-DD for date and HH:mm for time.";
        } catch (Exception e) {
            LOGGER.error("Tool createReservation: unexpected error", e);
            return "An unexpected error occurred while creating the reservation. Please try again.";
        }
    }

    @Tool("Checks the status of an existing reservation by its reference number. " +
            "Returns reservation details on success, or an error message if not found.")
    public String checkReservation(
            @P("Establishment identifier (UUID) from the context") String establishmentId,
            @P("Session identifier of the user checking the reservation, from the context") String sessionId,
            @P("Reservation reference number (e.g. A1B2C3D4)") String referenceNumber) {
        LOGGER.info("Tool checkReservation: establishmentId={}, sessionId={}, referenceNumber={}",
                establishmentId, sessionId, referenceNumber);
        try {
            Optional<ReservationRepositoryPort.ReservationSummary> found =
                    reservationRepository.findReservation(referenceNumber.trim().toUpperCase(Locale.ROOT));
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
            @P("Session identifier of the user cancelling the reservation, from the context") String sessionId,
            @P("Reservation reference number to cancel (e.g. A1B2C3D4)") String referenceNumber) {
        LOGGER.info("Tool cancelReservation: establishmentId={}, sessionId={}, referenceNumber={}",
                establishmentId, sessionId, referenceNumber);
        try {
            ReservationRepositoryPort.ReservationResult result =
                    reservationRepository.cancelReservation(referenceNumber.trim().toUpperCase(Locale.ROOT));
            if (result.success()) {
                LOGGER.info("Tool cancelReservation: success, establishmentId={}, sessionId={}, reference={}",
                        establishmentId, sessionId, result.referenceNumber());
                return "Reservation " + result.referenceNumber() + " has been successfully cancelled.";
            }
            LOGGER.warn("Tool cancelReservation: failure, establishmentId={}, sessionId={}, message={}",
                    establishmentId, sessionId, result.message());
            return "Failed to cancel reservation: " + result.message();
        } catch (Exception e) {
            LOGGER.error("Tool cancelReservation: unexpected error", e);
            return "An unexpected error occurred while cancelling the reservation. Please try again.";
        }
    }
}
