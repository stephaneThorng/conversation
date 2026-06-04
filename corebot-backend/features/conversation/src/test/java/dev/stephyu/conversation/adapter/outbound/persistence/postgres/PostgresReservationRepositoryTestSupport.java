package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_CLOSURE;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_RESERVATION_TABLE_MAP;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stephyu.conversation.application.port.outbound.ReservationRepositoryPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@NullMarked
abstract class PostgresReservationRepositoryTestSupport {

    protected static final UUID ESTABLISHMENT_ID = UUID.fromString("7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201");
    protected static final UUID TABLE_01 = UUID.fromString("b2000000-0000-0000-0000-000000000001");
    protected static final UUID TABLE_02 = UUID.fromString("b2000000-0000-0000-0000-000000000002");
    protected static final UUID TABLE_09 = UUID.fromString("b2000000-0000-0000-0000-000000000009");

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    protected DSLContext dsl;
    protected ReservationRepositoryPort repository;
    @Nullable
    private LocalDate temporaryClosureDate;

    @BeforeAll
    static void startContainer() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    @AfterAll
    static void stopContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @BeforeEach
    void setUpDatabase() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        dsl = DSL.using(dataSource, SQLDialect.POSTGRES);
        clearReservationState();
        repository = new PostgresReservationRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        if (temporaryClosureDate != null) {
            dsl.deleteFrom(RESTAURANT_CLOSURE)
                    .where(RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(ESTABLISHMENT_ID))
                    .and(RESTAURANT_CLOSURE.CLOSURE_DATE.eq(temporaryClosureDate))
                    .execute();
            temporaryClosureDate = null;
        }
        clearReservationState();
    }

    protected final ReservationRepositoryPort.CreateReservationRequest request(
            LocalDate date, LocalTime time, int peopleCount) {
        return new ReservationRepositoryPort.CreateReservationRequest(
                ESTABLISHMENT_ID.toString(),
                "channel-user-1",
                "Alice",
                date,
                time,
                peopleCount);
    }

    protected final LocalDate nextDateFor(DayOfWeek desiredDayOfWeek) {
        for (int offset = 0; offset < 21; offset++) {
            LocalDate candidate = LocalDate.now(ZoneId.systemDefault()).plusDays(offset);
            if (candidate.getDayOfWeek() == desiredDayOfWeek) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to find a matching date");
    }

    protected final void createTemporaryClosure(LocalDate date) {
        temporaryClosureDate = date;
        dsl.insertInto(RESTAURANT_CLOSURE)
                .set(RESTAURANT_CLOSURE.ID, UUID.randomUUID())
                .set(RESTAURANT_CLOSURE.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_CLOSURE.CLOSURE_DATE, date)
                .set(RESTAURANT_CLOSURE.REASON, "temporary test closure")
                .set(RESTAURANT_CLOSURE.ACTIVE, true)
                .execute();
        assertTrue(dsl.fetchExists(
                RESTAURANT_CLOSURE,
                RESTAURANT_CLOSURE.ESTABLISHMENT_ID.eq(ESTABLISHMENT_ID)
                        .and(RESTAURANT_CLOSURE.CLOSURE_DATE.eq(date))));
    }

    private void clearReservationState() {
        if (dsl == null) {
            return;
        }
        dsl.deleteFrom(RESTAURANT_RESERVATION_TABLE_MAP).execute();
        dsl.deleteFrom(RESTAURANT_RESERVATION).execute();
    }
}
