/* [jooq ignore start] */
create extension if not exists pgcrypto;
create extension if not exists pg_trgm;
/* [jooq ignore stop] */

create table restaurant_establishment (
    id uuid primary key default gen_random_uuid(),
    code varchar(255) unique not null,
    default_locale varchar(16) not null default 'en',
    name_translations jsonb not null,
    description_translations jsonb not null default '{}',
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table restaurant_menu (
    id uuid primary key default gen_random_uuid(),
    establishment_id uuid not null references restaurant_establishment(id) on delete cascade,
    code varchar(255) not null,
    name_translations jsonb not null,
    description_translations jsonb not null default '{}',
    active boolean not null default true,
    sort_order integer not null default 0,
    price_cents integer,
    currency char(3) not null default 'EUR',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (establishment_id, code),
    check (price_cents is null or price_cents >= 0)
);

create table restaurant_menu_section (
    id uuid primary key default gen_random_uuid(),
    menu_id uuid not null references restaurant_menu(id) on delete cascade,
    code varchar(255) not null,
    name_translations jsonb not null,
    description_translations jsonb not null default '{}',
    sort_order integer not null default 0,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (menu_id, code)
);

create table restaurant_menu_item (
    id uuid primary key default gen_random_uuid(),
    establishment_id uuid not null references restaurant_establishment(id) on delete cascade,
    code varchar(255) not null,
    name_translations jsonb not null,
    description_translations jsonb not null default '{}',
    ingredient_note_translations jsonb not null default '{}',
    price_cents integer not null,
    currency char(3) not null default 'EUR',
    category_codes text[] not null default '{}',
    allergen_codes text[] not null default '{}',
    dietary_restriction_codes text[] not null default '{}',
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (establishment_id, code),
    check (price_cents >= 0)
);

create table restaurant_menu_section_item_map (
    menu_section_id uuid not null references restaurant_menu_section(id) on delete cascade,
    menu_item_id uuid not null references restaurant_menu_item(id) on delete cascade,
    sort_order integer not null default 0,
    price_cents_override integer,
    primary key (menu_section_id, menu_item_id),
    check (price_cents_override is null or price_cents_override >= 0)
);

create table restaurant_menu_item_category (
    code varchar(255) primary key,
    name_translations jsonb not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table restaurant_allergen (
    code varchar(255) primary key,
    name_translations jsonb not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table restaurant_dietary_restriction (
    code varchar(255) primary key,
    name_translations jsonb not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index restaurant_menu_establishment_idx
    on restaurant_menu (establishment_id);

create index restaurant_menu_section_menu_idx
    on restaurant_menu_section (menu_id, sort_order);

create index restaurant_menu_item_establishment_idx
    on restaurant_menu_item (establishment_id, active);

create index restaurant_menu_section_item_map_section_idx
    on restaurant_menu_section_item_map (menu_section_id, sort_order);

/* [jooq ignore start] */
create index restaurant_menu_item_category_codes_gin_idx
    on restaurant_menu_item using gin (category_codes);

create index restaurant_menu_item_allergen_codes_gin_idx
    on restaurant_menu_item using gin (allergen_codes);

create index restaurant_menu_item_dietary_codes_gin_idx
    on restaurant_menu_item using gin (dietary_restriction_codes);
/* [jooq ignore stop] */

/* [jooq ignore start] */
create index restaurant_menu_name_translations_trgm_idx
    on restaurant_menu using gin ((name_translations::text) gin_trgm_ops);

create index restaurant_menu_item_name_translations_trgm_idx
    on restaurant_menu_item using gin ((name_translations::text) gin_trgm_ops);

create index restaurant_menu_item_ingredient_note_translations_trgm_idx
    on restaurant_menu_item using gin ((ingredient_note_translations::text) gin_trgm_ops);
/* [jooq ignore stop] */
