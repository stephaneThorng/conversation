package dev.stephyu.conversation.adapter.outbound.persistence.postgres;

import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_ESTABLISHMENT;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_ITEM;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_SECTION;
import static dev.stephyu.conversation.jooq.generated.Tables.RESTAURANT_MENU_SECTION_ITEM_MAP;

import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@NullMarked
abstract class PostgresMenuSearchRepositoryTestSupport {

    protected static final UUID ESTABLISHMENT_ID = UUID.fromString("6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b001");
    protected static final UUID OTHER_ESTABLISHMENT_ID = UUID.fromString("7f7d2c8e-2f55-4e75-8c9a-2f31fdd9b002");
    protected static final UUID BRUNCH_MENU_ID = UUID.fromString("43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d101");
    protected static final UUID DRINKS_MENU_ID = UUID.fromString("43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d102");
    protected static final UUID TASTING_MENU_ID = UUID.fromString("43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d103");
    protected static final UUID OTHER_MENU_ID = UUID.fromString("43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d104");
    protected static final UUID STARTER_SECTION_ID = UUID.fromString("1d3c6f30-58ab-4ff4-bbb2-95e4c8652101");
    protected static final UUID MAIN_SECTION_ID = UUID.fromString("1d3c6f30-58ab-4ff4-bbb2-95e4c8652102");
    protected static final UUID DRINK_SECTION_ID = UUID.fromString("1d3c6f30-58ab-4ff4-bbb2-95e4c8652103");
    protected static final UUID PAPAYA_ITEM_ID = UUID.fromString("4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093001");
    protected static final UUID MISO_ITEM_ID = UUID.fromString("4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093002");
    protected static final UUID WAGYU_ITEM_ID = UUID.fromString("4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093003");
    protected static final UUID SPRITZ_ITEM_ID = UUID.fromString("4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093004");
    protected static final UUID OTHER_PAPAYA_ITEM_ID = UUID.fromString("4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093005");

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    protected DSLContext dsl;
    protected SearchMenuRepositoryPort repository;

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
        dsl.truncate(
                        RESTAURANT_MENU_SECTION_ITEM_MAP,
                        RESTAURANT_MENU_SECTION,
                        RESTAURANT_MENU_ITEM,
                        RESTAURANT_MENU,
                        RESTAURANT_ESTABLISHMENT)
                .cascade()
                .execute();
        seedData();
        repository = new PostgresMenuSearchRepository(dsl);
    }

    protected final void seedData() {
        dsl.insertInto(RESTAURANT_ESTABLISHMENT)
                .set(RESTAURANT_ESTABLISHMENT.ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_ESTABLISHMENT.CODE, "koru_kulture_bali")
                .set(RESTAURANT_ESTABLISHMENT.DEFAULT_LOCALE, "en")
                .set(RESTAURANT_ESTABLISHMENT.NAME_TRANSLATIONS, json("{\"en\":\"Koru Kulture\",\"fr\":\"Koru Kulture\"}"))
                .set(RESTAURANT_ESTABLISHMENT.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Modern Asian fusion restaurant\"}"))
                .execute();
        dsl.insertInto(RESTAURANT_ESTABLISHMENT)
                .set(RESTAURANT_ESTABLISHMENT.ID, OTHER_ESTABLISHMENT_ID)
                .set(RESTAURANT_ESTABLISHMENT.CODE, "other_place")
                .set(RESTAURANT_ESTABLISHMENT.DEFAULT_LOCALE, "en")
                .set(RESTAURANT_ESTABLISHMENT.NAME_TRANSLATIONS, json("{\"en\":\"Other Place\"}"))
                .set(RESTAURANT_ESTABLISHMENT.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Another venue\"}"))
                .execute();

        dsl.insertInto(RESTAURANT_MENU)
                .set(RESTAURANT_MENU.ID, BRUNCH_MENU_ID)
                .set(RESTAURANT_MENU.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU.CODE, "brunch_menu")
                .set(RESTAURANT_MENU.NAME_TRANSLATIONS, json("{\"en\":\"Weekend Brunch\",\"fr\":\"Brunch du week-end\"}"))
                .set(RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Saturday and Sunday\"}"))
                .set(RESTAURANT_MENU.SORT_ORDER, 1)
                .set(RESTAURANT_MENU.PRICE_CENTS, 2500)
                .execute();
        dsl.insertInto(RESTAURANT_MENU)
                .set(RESTAURANT_MENU.ID, DRINKS_MENU_ID)
                .set(RESTAURANT_MENU.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU.CODE, "drinks_menu")
                .set(RESTAURANT_MENU.NAME_TRANSLATIONS, json("{\"en\":\"Drinks\"}"))
                .set(RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Cocktails and softs\"}"))
                .set(RESTAURANT_MENU.SORT_ORDER, 2)
                .set(RESTAURANT_MENU.PRICE_CENTS, 1500)
                .execute();
        dsl.insertInto(RESTAURANT_MENU)
                .set(RESTAURANT_MENU.ID, TASTING_MENU_ID)
                .set(RESTAURANT_MENU.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU.CODE, "tasting_menu")
                .set(RESTAURANT_MENU.NAME_TRANSLATIONS, json("{\"en\":\"Chef Tasting\"}"))
                .set(RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Signature journey\"}"))
                .set(RESTAURANT_MENU.SORT_ORDER, 3)
                .set(RESTAURANT_MENU.PRICE_CENTS, 6000)
                .execute();
        dsl.insertInto(RESTAURANT_MENU)
                .set(RESTAURANT_MENU.ID, OTHER_MENU_ID)
                .set(RESTAURANT_MENU.ESTABLISHMENT_ID, OTHER_ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU.CODE, "other_brunch")
                .set(RESTAURANT_MENU.NAME_TRANSLATIONS, json("{\"en\":\"Weekend Brunch\"}"))
                .set(RESTAURANT_MENU.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Should stay hidden\"}"))
                .set(RESTAURANT_MENU.SORT_ORDER, 1)
                .set(RESTAURANT_MENU.PRICE_CENTS, 2500)
                .execute();

        dsl.insertInto(RESTAURANT_MENU_SECTION)
                .set(RESTAURANT_MENU_SECTION.ID, STARTER_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION.MENU_ID, BRUNCH_MENU_ID)
                .set(RESTAURANT_MENU_SECTION.CODE, "starter")
                .set(RESTAURANT_MENU_SECTION.NAME_TRANSLATIONS, json("{\"en\":\"Starters\"}"))
                .set(RESTAURANT_MENU_SECTION.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"To begin\"}"))
                .set(RESTAURANT_MENU_SECTION.SORT_ORDER, 1)
                .execute();
        dsl.insertInto(RESTAURANT_MENU_SECTION)
                .set(RESTAURANT_MENU_SECTION.ID, MAIN_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION.MENU_ID, BRUNCH_MENU_ID)
                .set(RESTAURANT_MENU_SECTION.CODE, "main")
                .set(RESTAURANT_MENU_SECTION.NAME_TRANSLATIONS, json("{\"en\":\"Mains\"}"))
                .set(RESTAURANT_MENU_SECTION.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Main plates\"}"))
                .set(RESTAURANT_MENU_SECTION.SORT_ORDER, 2)
                .execute();
        dsl.insertInto(RESTAURANT_MENU_SECTION)
                .set(RESTAURANT_MENU_SECTION.ID, DRINK_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION.MENU_ID, DRINKS_MENU_ID)
                .set(RESTAURANT_MENU_SECTION.CODE, "cocktail")
                .set(RESTAURANT_MENU_SECTION.NAME_TRANSLATIONS, json("{\"en\":\"Cocktails\"}"))
                .set(RESTAURANT_MENU_SECTION.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Mixes\"}"))
                .set(RESTAURANT_MENU_SECTION.SORT_ORDER, 1)
                .execute();

        dsl.insertInto(RESTAURANT_MENU_ITEM)
                .set(RESTAURANT_MENU_ITEM.ID, PAPAYA_ITEM_ID)
                .set(RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU_ITEM.CODE, "green_papaya_salad")
                .set(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS, json("{\"en\":\"Green Papaya Salad\",\"fr\":\"Salade de papaye verte\"}"))
                .set(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Fresh and bright\"}"))
                .set(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, json("{\"en\":\"Green papaya, lime dressing, peanuts\"}"))
                .set(RESTAURANT_MENU_ITEM.PRICE_CENTS, 1850)
                .set(RESTAURANT_MENU_ITEM.CATEGORY_CODES, new String[] { "starter", "thai" })
                .set(RESTAURANT_MENU_ITEM.ALLERGEN_CODES, new String[] { "nut" })
                .set(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES, new String[] { "gluten_free" })
                .execute();
        dsl.insertInto(RESTAURANT_MENU_ITEM)
                .set(RESTAURANT_MENU_ITEM.ID, MISO_ITEM_ID)
                .set(RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU_ITEM.CODE, "miso_soup")
                .set(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS, json("{\"en\":\"Miso Soup\"}"))
                .set(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Silken tofu broth\"}"))
                .set(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, json("{\"en\":\"Miso, tofu, spring onion\"}"))
                .set(RESTAURANT_MENU_ITEM.PRICE_CENTS, 950)
                .set(RESTAURANT_MENU_ITEM.CATEGORY_CODES, new String[] { "starter" })
                .set(RESTAURANT_MENU_ITEM.ALLERGEN_CODES, new String[] { "soy" })
                .set(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES, new String[] { "vegan" })
                .execute();
        dsl.insertInto(RESTAURANT_MENU_ITEM)
                .set(RESTAURANT_MENU_ITEM.ID, WAGYU_ITEM_ID)
                .set(RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU_ITEM.CODE, "wagyu_burger")
                .set(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS, json("{\"en\":\"Wagyu Burger\"}"))
                .set(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Rich and indulgent\"}"))
                .set(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, json("{\"en\":\"Wagyu beef, brioche, pickles\"}"))
                .set(RESTAURANT_MENU_ITEM.PRICE_CENTS, 2450)
                .set(RESTAURANT_MENU_ITEM.CATEGORY_CODES, new String[] { "main" })
                .set(RESTAURANT_MENU_ITEM.ALLERGEN_CODES, new String[] { "gluten" })
                .set(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES, new String[] { "halal" })
                .execute();
        dsl.insertInto(RESTAURANT_MENU_ITEM)
                .set(RESTAURANT_MENU_ITEM.ID, SPRITZ_ITEM_ID)
                .set(RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID, ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU_ITEM.CODE, "lychee_spritz")
                .set(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS, json("{\"en\":\"Lychee Spritz\"}"))
                .set(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Sparkling cocktail\"}"))
                .set(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, json("{\"en\":\"Lychee, prosecco, soda\"}"))
                .set(RESTAURANT_MENU_ITEM.PRICE_CENTS, 1200)
                .set(RESTAURANT_MENU_ITEM.CATEGORY_CODES, new String[] { "cocktail" })
                .set(RESTAURANT_MENU_ITEM.ALLERGEN_CODES, new String[0])
                .set(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES, new String[] { "vegan" })
                .execute();
        dsl.insertInto(RESTAURANT_MENU_ITEM)
                .set(RESTAURANT_MENU_ITEM.ID, OTHER_PAPAYA_ITEM_ID)
                .set(RESTAURANT_MENU_ITEM.ESTABLISHMENT_ID, OTHER_ESTABLISHMENT_ID)
                .set(RESTAURANT_MENU_ITEM.CODE, "other_papaya")
                .set(RESTAURANT_MENU_ITEM.NAME_TRANSLATIONS, json("{\"en\":\"Green Papaya Salad\"}"))
                .set(RESTAURANT_MENU_ITEM.DESCRIPTION_TRANSLATIONS, json("{\"en\":\"Should stay hidden\"}"))
                .set(RESTAURANT_MENU_ITEM.INGREDIENT_NOTE_TRANSLATIONS, json("{\"en\":\"Papaya, lime\"}"))
                .set(RESTAURANT_MENU_ITEM.PRICE_CENTS, 999)
                .set(RESTAURANT_MENU_ITEM.CATEGORY_CODES, new String[] { "starter" })
                .set(RESTAURANT_MENU_ITEM.ALLERGEN_CODES, new String[] { "nut" })
                .set(RESTAURANT_MENU_ITEM.DIETARY_RESTRICTION_CODES, new String[] { "vegan" })
                .execute();

        dsl.insertInto(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID, STARTER_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID, PAPAYA_ITEM_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.SORT_ORDER, 1)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.PRICE_CENTS_OVERRIDE, 1590)
                .execute();
        dsl.insertInto(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID, STARTER_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID, MISO_ITEM_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.SORT_ORDER, 2)
                .execute();
        dsl.insertInto(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID, MAIN_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID, WAGYU_ITEM_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.SORT_ORDER, 1)
                .execute();
        dsl.insertInto(RESTAURANT_MENU_SECTION_ITEM_MAP)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_SECTION_ID, DRINK_SECTION_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.MENU_ITEM_ID, SPRITZ_ITEM_ID)
                .set(RESTAURANT_MENU_SECTION_ITEM_MAP.SORT_ORDER, 1)
                .execute();
    }

    private static JSON json(String value) {
        return JSON.json(value);
    }
}
