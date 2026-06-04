package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_ESTABLISHMENT;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_OPENING_HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.stephyu.conversation.adapter.outbound.llm.OpeningHoursTools;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.jooq.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class OpeningHoursToolsTest extends PostgresReservationRepositoryTestSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OpeningHoursTools tools;

    @BeforeEach
    void setUpTools() {
        tools = new OpeningHoursTools(new PostgresRestaurantAvailabilityRepository(dsl));
    }

    @Test
    void shouldReturnWeeklyHoursAndUpcomingClosures() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(tools.getOpeningHours(ESTABLISHMENT_ID.toString(), "", ""));

        assertEquals(ESTABLISHMENT_ID.toString(), payload.get("establishmentId").asText());
        assertEquals(7, payload.get("weeklyOpeningHours").size());
        assertEquals("MONDAY", payload.get("weeklyOpeningHours").get(0).get("dayName").asText());
        assertTrue(payload.get("upcomingClosures").size() >= 3);
        assertTrue(payload.get("requestedAvailability").isNull());
    }

    @Test
    void shouldReportOpenAndClosedDates() throws Exception {
        LocalDate openDate = nextDateFor(DayOfWeek.WEDNESDAY);
        JsonNode openPayload = OBJECT_MAPPER.readTree(
                tools.getOpeningHours(ESTABLISHMENT_ID.toString(), openDate.toString(), "19:30"));

        JsonNode openAvailability = openPayload.get("requestedAvailability");
        assertEquals("OPEN", openAvailability.get("status").asText());
        assertTrue(openAvailability.get("openForDate").asBoolean());
        assertTrue(openAvailability.get("openForTime").asBoolean());

        LocalDate closedDate = nextDateFor(DayOfWeek.MONDAY);
        JsonNode closedPayload = OBJECT_MAPPER.readTree(
                tools.getOpeningHours(ESTABLISHMENT_ID.toString(), closedDate.toString(), "19:30"));

        JsonNode closedAvailability = closedPayload.get("requestedAvailability");
        assertEquals("CLOSED", closedAvailability.get("status").asText());
        assertFalse(closedAvailability.get("openForDate").asBoolean());
        assertFalse(closedAvailability.get("openForTime").asBoolean());
    }

    @Test
    void shouldRespectClosuresAndIgnoreOtherEstablishments() throws Exception {
        LocalDate closureDate = nextDateFor(DayOfWeek.THURSDAY);
        createTemporaryClosure(closureDate);
        UUID otherEstablishmentId = insertOtherEstablishmentData();
        try {
            JsonNode payload = OBJECT_MAPPER.readTree(
                    tools.getOpeningHours(ESTABLISHMENT_ID.toString(), closureDate.toString(), "19:30"));

            JsonNode availability = payload.get("requestedAvailability");
            assertEquals("CLOSED", availability.get("status").asText());
            assertEquals("closure", availability.get("reason").asText());
            assertEquals(closureDate.toString(), availability.get("closure").get("closureDate").asText());
            assertFalse(payload.toString().contains("other-establishment"));
            assertFalse(payload.toString().contains("temporary other establishment closure"));
        } finally {
            dsl.deleteFrom(RESTAURANT_CLOSURE)
                    .where(RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(otherEstablishmentId))
                    .execute();
            dsl.deleteFrom(RESTAURANT_OPENING_HOURS)
                    .where(RESTAURANT_OPENING_HOURS.ESTABLISHMENT_ID.eq(otherEstablishmentId))
                    .execute();
            dsl.deleteFrom(RESTAURANT_ESTABLISHMENT)
                    .where(RESTAURANT_ESTABLISHMENT.ID.eq(otherEstablishmentId))
                    .execute();
        }
    }

    @Test
    void shouldRejectTimeWithoutDate() {
        String message = tools.getOpeningHours(ESTABLISHMENT_ID.toString(), "", "19:30");

        assertTrue(message.contains("Invalid opening hours request"));
    }

    private UUID insertOtherEstablishmentData() {
        UUID otherEstablishmentId = UUID.fromString("7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c299");
        dsl.insertInto(RESTAURANT_ESTABLISHMENT)
                .set(RESTAURANT_ESTABLISHMENT.ID, otherEstablishmentId)
                .set(RESTAURANT_ESTABLISHMENT.CODE, "other-establishment")
                .set(RESTAURANT_ESTABLISHMENT.DEFAULT_LOCALE, "en")
                .set(RESTAURANT_ESTABLISHMENT.NAME_TRANSLATIONS, JSON.json("{\"en\":\"Other Establishment\"}"))
                .set(RESTAURANT_ESTABLISHMENT.DESCRIPTION_TRANSLATIONS, JSON.json("{\"en\":\"Other venue\"}"))
                .execute();

        dsl.insertInto(RESTAURANT_OPENING_HOURS)
                .set(RESTAURANT_OPENING_HOURS.ID, UUID.randomUUID())
                .set(RESTAURANT_OPENING_HOURS.ESTABLISHMENT_ID, otherEstablishmentId)
                .set(RESTAURANT_OPENING_HOURS.DAY_OF_WEEK, (short) 1)
                .set(RESTAURANT_OPENING_HOURS.OPEN_TIME, LocalTime.of(10, 0))
                .set(RESTAURANT_OPENING_HOURS.CLOSE_TIME, LocalTime.of(23, 0))
                .set(RESTAURANT_OPENING_HOURS.ACTIVE, true)
                .execute();

        dsl.insertInto(RESTAURANT_CLOSURE)
                .set(RESTAURANT_CLOSURE.ID, UUID.randomUUID())
                .set(RESTAURANT_CLOSURE.ESTABLISHMENT_ID, otherEstablishmentId)
                .set(RESTAURANT_CLOSURE.CLOSURE_DATE, nextDateFor(DayOfWeek.FRIDAY))
                .set(RESTAURANT_CLOSURE.REASON, "temporary other establishment closure")
                .set(RESTAURANT_CLOSURE.ACTIVE, true)
                .execute();
        return otherEstablishmentId;
    }
}
