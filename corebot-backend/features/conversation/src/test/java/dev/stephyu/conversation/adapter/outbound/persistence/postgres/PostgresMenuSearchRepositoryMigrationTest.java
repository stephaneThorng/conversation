package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class PostgresMenuSearchRepositoryMigrationTest extends PostgresMenuSearchRepositoryTestSupport {

    @Test
    void shouldCreateRestaurantTablesAndExtensions() {
        assertEquals("restaurant_menu", dsl.fetchValue("select to_regclass('public.restaurant_menu')"));
        assertEquals("restaurant_menu_item", dsl.fetchValue("select to_regclass('public.restaurant_menu_item')"));

        List<String> extensions = dsl.fetch("select extname from pg_extension where extname in ('pgcrypto', 'pg_trgm')")
                .getValues(0, String.class);
        assertTrue(extensions.contains("pgcrypto"));
        assertTrue(extensions.contains("pg_trgm"));
    }
}
