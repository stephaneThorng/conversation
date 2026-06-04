/* [jooq ignore start] */
create extension if not exists pgcrypto;
create extension if not exists pg_trgm;
/* [jooq ignore stop] */

-- ============================================================
-- ESTABLISHMENT
-- ============================================================

create table restaurant_establishment (
    id                       uuid         primary key default gen_random_uuid(),
    code                     varchar(255) not null unique,
    default_locale           varchar(16)  not null default 'en',
    name_translations        jsonb        not null,
    description_translations jsonb        not null default '{}',
    active                   boolean      not null default true,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now()
);

-- ============================================================
-- MENU REFERENTIALS
-- ============================================================

create table restaurant_menu_item_category (
    code              varchar(255) primary key,
    name_translations jsonb        not null,
    active            boolean      not null default true,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

create table restaurant_allergen (
    code              varchar(255) primary key,
    name_translations jsonb        not null,
    active            boolean      not null default true,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

create table restaurant_dietary_restriction (
    code              varchar(255) primary key,
    name_translations jsonb        not null,
    active            boolean      not null default true,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

-- ============================================================
-- MENU
-- ============================================================

create table restaurant_menu (
    id                       uuid         primary key default gen_random_uuid(),
    establishment_id         uuid         not null references restaurant_establishment(id) on delete cascade,
    code                     varchar(255) not null,
    name_translations        jsonb        not null,
    description_translations jsonb        not null default '{}',
    active                   boolean      not null default true,
    sort_order               integer      not null default 0,
    price_cents              integer,
    currency                 char(3)      not null default 'EUR',
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now(),
    unique (establishment_id, code),
    check (price_cents is null or price_cents >= 0)
);

create table restaurant_menu_section (
    id                       uuid         primary key default gen_random_uuid(),
    menu_id                  uuid         not null references restaurant_menu(id) on delete cascade,
    code                     varchar(255) not null,
    name_translations        jsonb        not null,
    description_translations jsonb        not null default '{}',
    sort_order               integer      not null default 0,
    active                   boolean      not null default true,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now(),
    unique (menu_id, code)
);

create table restaurant_menu_item (
    id                           uuid         primary key default gen_random_uuid(),
    establishment_id             uuid         not null references restaurant_establishment(id) on delete cascade,
    code                         varchar(255) not null,
    name_translations            jsonb        not null,
    description_translations     jsonb        not null default '{}',
    ingredient_note_translations jsonb        not null default '{}',
    price_cents                  integer      not null,
    currency                     char(3)      not null default 'EUR',
    category_codes               text[]       not null default '{}',
    allergen_codes               text[]       not null default '{}',
    dietary_restriction_codes    text[]       not null default '{}',
    active                       boolean      not null default true,
    created_at                   timestamptz  not null default now(),
    updated_at                   timestamptz  not null default now(),
    unique (establishment_id, code),
    check (price_cents >= 0)
);

create table restaurant_menu_section_item_map (
    menu_section_id      uuid    not null references restaurant_menu_section(id) on delete cascade,
    menu_item_id         uuid    not null references restaurant_menu_item(id) on delete cascade,
    sort_order           integer not null default 0,
    price_cents_override integer,
    primary key (menu_section_id, menu_item_id),
    check (price_cents_override is null or price_cents_override >= 0)
);

-- ============================================================
-- RESERVATION CONFIG (1-1 per establishment)
-- ============================================================

create table restaurant_reservation_config (
    establishment_id             uuid        primary key references restaurant_establishment(id) on delete cascade,
    -- Duration in minutes a reservation occupies. Mandatory, defined by the establishment.
    -- Copied into each reservation row so historical records remain accurate if config changes.
    reservation_duration_minutes integer     not null check (reservation_duration_minutes > 0),
    -- Enables merging multiple tables to cover a larger group (max 3 tables enforced applicatively).
    allow_table_merging          boolean     not null default false,
    -- Maximum number of days in advance a reservation can be booked.
    max_advance_days             integer     not null check (max_advance_days > 0),
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);

-- ============================================================
-- OPENING HOURS
-- Multiple rows per (establishment_id, day_of_week) are allowed
-- to model split shifts (e.g. lunch 11:30-14:30 + dinner 18:30-22:00).
-- day_of_week uses Java DayOfWeek ISO convention:
--   1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday,
--   5 = Friday, 6 = Saturday, 7 = Sunday.
-- A day with no active row means the establishment is closed that day.
-- active = false disables a slot temporarily without deleting it.
-- ============================================================

create table restaurant_opening_hours (
    id               uuid      primary key default gen_random_uuid(),
    establishment_id uuid      not null references restaurant_establishment(id) on delete cascade,
    day_of_week      smallint  not null check (day_of_week between 1 and 7),
    open_time        time      not null,
    close_time       time      not null,
    active           boolean   not null default true,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    check (close_time > open_time)
);

-- ============================================================
-- CLOSURES (exceptional closing dates)
-- ============================================================

create table restaurant_closure (
    id               uuid         primary key default gen_random_uuid(),
    establishment_id uuid         not null references restaurant_establishment(id) on delete cascade,
    closure_date     date         not null,
    reason           varchar(500),
    active           boolean      not null default true,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),
    unique (establishment_id, closure_date)
);

-- ============================================================
-- PHYSICAL TABLES
-- ============================================================

create table restaurant_table (
    id               uuid         primary key default gen_random_uuid(),
    establishment_id uuid         not null references restaurant_establishment(id) on delete cascade,
    table_number     varchar(50)  not null,
    seat_count       integer      not null check (seat_count >= 1),
    active           boolean      not null default true,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),
    unique (establishment_id, table_number)
);

-- ============================================================
-- RESERVATION
-- status lifecycle: PENDING -> CONFIRMED | CANCELLED
-- duration_minutes is copied from restaurant_reservation_config at booking time.
-- ============================================================

create table restaurant_reservation (
    id               uuid         primary key default gen_random_uuid(),
    establishment_id uuid         not null references restaurant_establishment(id) on delete cascade,
    reference_number varchar(20)  not null unique,
    channel_user_id  varchar(255) not null,
    reservation_name varchar(255) not null,
    date             date         not null,
    time             time         not null,
    duration_minutes integer      not null check (duration_minutes > 0),
    people_count     integer      not null check (people_count >= 1),
    status           varchar(20)  not null default 'PENDING'
                                  check (status in ('PENDING', 'CONFIRMED', 'CANCELLED')),
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now()
);

-- ============================================================
-- RESERVATION <-> TABLE allocation
-- Links a reservation to one or more physical tables.
-- The applicative layer enforces the max 3 tables per merged reservation rule.
-- ============================================================

create table restaurant_reservation_table_map (
    reservation_id uuid not null references restaurant_reservation(id) on delete cascade,
    table_id       uuid not null references restaurant_table(id) on delete cascade,
    primary key (reservation_id, table_id)
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Menu
create index idx_restaurant_menu_establishment
    on restaurant_menu (establishment_id);

create index idx_restaurant_menu_section_menu_sort
    on restaurant_menu_section (menu_id, sort_order);

create index idx_restaurant_menu_item_establishment_active
    on restaurant_menu_item (establishment_id, active);

create index idx_restaurant_menu_section_item_map_section_sort
    on restaurant_menu_section_item_map (menu_section_id, sort_order);

/* [jooq ignore start] */
create index idx_restaurant_menu_item_category_codes_gin
    on restaurant_menu_item using gin (category_codes);

create index idx_restaurant_menu_item_allergen_codes_gin
    on restaurant_menu_item using gin (allergen_codes);

create index idx_restaurant_menu_item_dietary_codes_gin
    on restaurant_menu_item using gin (dietary_restriction_codes);

create index idx_restaurant_menu_name_trgm
    on restaurant_menu using gin ((name_translations::text) gin_trgm_ops);

create index idx_restaurant_menu_item_name_trgm
    on restaurant_menu_item using gin ((name_translations::text) gin_trgm_ops);

create index idx_restaurant_menu_item_ingredient_trgm
    on restaurant_menu_item using gin ((ingredient_note_translations::text) gin_trgm_ops);
/* [jooq ignore stop] */

-- Opening hours
create index idx_restaurant_opening_hours_establishment_day
    on restaurant_opening_hours (establishment_id, day_of_week);

-- Closures
create index idx_restaurant_closure_establishment_date
    on restaurant_closure (establishment_id, closure_date);

-- Tables
create index idx_restaurant_table_establishment_active
    on restaurant_table (establishment_id, active);

-- Reservations
create index idx_restaurant_reservation_establishment_date_status
    on restaurant_reservation (establishment_id, date, status);

create index idx_restaurant_reservation_channel_user
    on restaurant_reservation (channel_user_id);

create index idx_restaurant_reservation_table_map_table_id
    on restaurant_reservation_table_map (table_id);
