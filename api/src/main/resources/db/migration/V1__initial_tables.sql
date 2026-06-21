CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
ALTER EXTENSION pg_trgm SET SCHEMA public;

CREATE TABLE IF NOT EXISTS media_assets
(
    id                 BIGINT PRIMARY KEY,

    type               VARCHAR(10) NOT NULL, -- image

    image_aspect_ratio VARCHAR(10) NOT NULL, -- 'square', 'wide'
    image_formats      JSONB       NOT NULL, -- JSON

    description        TEXT        NOT NULL
);

CREATE TABLE IF NOT EXISTS flags
(
    id              INTEGER PRIMARY KEY,

    caption         VARCHAR(255) NOT NULL,
    emoji           VARCHAR(16)  NOT NULL,
    emoji_u         VARCHAR(64)  NOT NULL,

    square_asset_id BIGINT REFERENCES media_assets (id),
    wide_asset_id   BIGINT REFERENCES media_assets (id)
);

CREATE TABLE IF NOT EXISTS regions
(
    id           INTEGER PRIMARY KEY,

    name         VARCHAR(255) NOT NULL,
    wiki_data_id VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS region_translations
(
    id            BIGSERIAL PRIMARY KEY,
    region_id     INTEGER      NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS subregions
(
    id           INTEGER PRIMARY KEY,

    name         VARCHAR(255) NOT NULL,
    region_id    INTEGER      NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    wiki_data_id VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS subregion_translations
(
    id            BIGSERIAL PRIMARY KEY,
    subregion_id  INTEGER      NOT NULL REFERENCES subregions (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS timezones
(
    id              INTEGER PRIMARY KEY,

    zone_name       VARCHAR(100) NOT NULL,

    gmt_offset      INTEGER      NOT NULL,
    gmt_offset_name VARCHAR(20)  NOT NULL,

    abbreviation    VARCHAR(10)  NOT NULL,

    tz_name         VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS timezone_translations
(
    id            BIGSERIAL PRIMARY KEY,
    timezone_id   INTEGER      NOT NULL REFERENCES timezones (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    tz_name       VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS currencies
(
    id              INTEGER PRIMARY KEY,

    iso3            VARCHAR(10)  NOT NULL,
    iso_numeric     VARCHAR(10)  NOT NULL,

    name            VARCHAR(100) NOT NULL,
    description     TEXT,

    native_name     VARCHAR(255),
    symbol          VARCHAR(10),

    year            INT,
    introduced_date DATE,

    obsolete        BOOLEAN      NOT NULL,
    obsolete_at     DATE,

    replaced_by     INTEGER      REFERENCES currencies (id) ON DELETE SET NULL,

    flag_id         INTEGER REFERENCES flags (id),

    decimal_digits  INTEGER      NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS currencies_iso_code_uk ON currencies (iso3);
CREATE UNIQUE INDEX IF NOT EXISTS currencies_iso_numeric_uk ON currencies (iso_numeric);

CREATE TABLE IF NOT EXISTS currency_translations
(
    id            BIGSERIAL PRIMARY KEY,

    currency_id   INTEGER      NOT NULL REFERENCES currencies (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL,
    description   TEXT
);

CREATE TABLE IF NOT EXISTS countries
(
    id           INTEGER PRIMARY KEY,

    name         VARCHAR(255)     NOT NULL,
    native_name  VARCHAR(255),

    iso3         VARCHAR(3)       NOT NULL,
    iso2         VARCHAR(2)       NOT NULL,
    iso_numeric  VARCHAR(3)       NOT NULL,

    phone_code   VARCHAR(5)       NOT NULL,
    tld          VARCHAR(10),

    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,

    flag_id      INTEGER REFERENCES flags (id),

    region_id    INTEGER          NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    subregion_id INTEGER          NOT NULL REFERENCES subregions (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS countries_iso3_uk ON countries (iso3);
CREATE UNIQUE INDEX IF NOT EXISTS countries_iso2_uk ON countries (iso2);
CREATE UNIQUE INDEX IF NOT EXISTS countries_iso_numeric_uk ON countries (iso_numeric);

CREATE TABLE IF NOT EXISTS country_translations
(
    id            BIGSERIAL PRIMARY KEY,

    country_id    INTEGER      NOT NULL REFERENCES countries (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS states
(
    id         BIGINT PRIMARY KEY,

    country_id INTEGER      NOT NULL REFERENCES countries (id) ON DELETE CASCADE,

    name       VARCHAR(255) NOT NULL,

    state_code VARCHAR(10),

    latitude   DOUBLE PRECISION,
    longitude  DOUBLE PRECISION,

    type       VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS state_translations
(
    id            BIGSERIAL PRIMARY KEY,

    state_id      BIGINT       NOT NULL REFERENCES states (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS cities
(
    id        BIGINT PRIMARY KEY,

    state_id  BIGINT           NOT NULL REFERENCES states (id) ON DELETE CASCADE,
    name      VARCHAR(255)     NOT NULL,

    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS city_translations
(
    id            BIGSERIAL PRIMARY KEY,

    city_id       BIGINT       NOT NULL REFERENCES cities (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS languages
(
    id          INTEGER PRIMARY KEY,

    code        VARCHAR(10)  NOT NULL,
    native_name VARCHAR(100),

    name        VARCHAR(100) NOT NULL,
    description TEXT,

    flag_id     INTEGER REFERENCES flags (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS languages_code_uk ON languages (code);

CREATE TABLE IF NOT EXISTS language_translations
(
    id            BIGSERIAL PRIMARY KEY,

    language_id   INTEGER      NOT NULL REFERENCES languages (id) ON DELETE CASCADE,
    language_code VARCHAR(10)  NOT NULL,

    name          VARCHAR(100) NOT NULL,
    description   TEXT
);

CREATE TABLE IF NOT EXISTS central_banks
(
    id                 INTEGER PRIMARY KEY,

    name               VARCHAR(100) NOT NULL,

    native_name        VARCHAR(255),
    website_url        VARCHAR(128),
    establishment_year INT
);

CREATE TABLE IF NOT EXISTS central_bank_translations
(
    id              BIGSERIAL PRIMARY KEY,

    central_bank_id INTEGER      NOT NULL REFERENCES central_banks (id) ON DELETE CASCADE,
    language_code   VARCHAR(10)  NOT NULL,

    name            VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS country_timezones
(
    country_id  INTEGER NOT NULL REFERENCES countries (id) ON DELETE CASCADE,
    timezone_id INTEGER NOT NULL REFERENCES timezones (id) ON DELETE CASCADE,
    PRIMARY KEY (country_id, timezone_id)
);

CREATE TABLE IF NOT EXISTS country_currencies
(
    country_id  INTEGER NOT NULL REFERENCES countries (id) ON DELETE CASCADE,
    currency_id INTEGER NOT NULL REFERENCES currencies (id) ON DELETE CASCADE,
    PRIMARY KEY (country_id, currency_id)
);

CREATE TABLE IF NOT EXISTS country_languages
(
    country_id  INTEGER     NOT NULL REFERENCES countries (id) ON DELETE CASCADE,
    language_id INTEGER     NOT NULL REFERENCES languages (id) ON DELETE CASCADE,
    type        VARCHAR(10) NOT NULL, -- official, other
    PRIMARY KEY (country_id, language_id)
);

CREATE TABLE IF NOT EXISTS central_bank_issued_currencies
(
    central_bank_id INTEGER NOT NULL REFERENCES central_banks (id) ON DELETE CASCADE,
    currency_id     INTEGER NOT NULL REFERENCES currencies (id) ON DELETE CASCADE,
    PRIMARY KEY (central_bank_id, currency_id)
);

CREATE TABLE IF NOT EXISTS central_bank_countries
(
    central_bank_id INTEGER NOT NULL REFERENCES central_banks (id) ON DELETE CASCADE,
    country_id      INTEGER NOT NULL REFERENCES countries (id) ON DELETE CASCADE,
    PRIMARY KEY (central_bank_id, country_id)
);

CREATE TABLE IF NOT EXISTS data_import_version
(
    id      INTEGER PRIMARY KEY,
    version INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS clients
(
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    access_key_hash   VARCHAR(64)  NOT NULL,
    access_key_prefix VARCHAR(32)  NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS media_assets_type_idx
    ON media_assets (type);
CREATE INDEX IF NOT EXISTS media_assets_description_trgm_idx
    ON media_assets USING gin (lower(description) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS flags_caption_idx
    ON flags (caption);
CREATE INDEX IF NOT EXISTS flags_square_asset_id_idx
    ON flags (square_asset_id);
CREATE INDEX IF NOT EXISTS flags_wide_asset_id_idx
    ON flags (wide_asset_id);
CREATE INDEX IF NOT EXISTS flags_caption_trgm_idx
    ON flags USING gin (lower(caption) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS regions_name_idx
    ON regions (name);
CREATE INDEX IF NOT EXISTS regions_wiki_data_id_idx
    ON regions (wiki_data_id);
CREATE INDEX IF NOT EXISTS regions_name_trgm_idx
    ON regions USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS region_translations_region_language_idx
    ON region_translations (region_id, language_code);
CREATE INDEX IF NOT EXISTS region_translations_language_name_idx
    ON region_translations (language_code, name);
CREATE INDEX IF NOT EXISTS region_translations_name_trgm_idx
    ON region_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS subregions_region_id_idx
    ON subregions (region_id);
CREATE INDEX IF NOT EXISTS subregions_name_idx
    ON subregions (name);
CREATE INDEX IF NOT EXISTS subregions_wiki_data_id_idx
    ON subregions (wiki_data_id);
CREATE INDEX IF NOT EXISTS subregions_name_trgm_idx
    ON subregions USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS subregion_translations_subregion_language_idx
    ON subregion_translations (subregion_id, language_code);
CREATE INDEX IF NOT EXISTS subregion_translations_language_name_idx
    ON subregion_translations (language_code, name);
CREATE INDEX IF NOT EXISTS subregion_translations_name_trgm_idx
    ON subregion_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS timezones_zone_name_idx
    ON timezones (zone_name);
CREATE INDEX IF NOT EXISTS timezones_tz_name_idx
    ON timezones (tz_name);
CREATE INDEX IF NOT EXISTS timezones_abbreviation_idx
    ON timezones (abbreviation);
CREATE INDEX IF NOT EXISTS timezones_tz_name_trgm_idx
    ON timezones USING gin (lower(tz_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS timezone_translations_timezone_language_idx
    ON timezone_translations (timezone_id, language_code);
CREATE INDEX IF NOT EXISTS timezone_translations_language_tz_name_idx
    ON timezone_translations (language_code, tz_name);
CREATE INDEX IF NOT EXISTS timezone_translations_tz_name_trgm_idx
    ON timezone_translations USING gin (lower(tz_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS currencies_name_idx
    ON currencies (name);
CREATE INDEX IF NOT EXISTS currencies_symbol_idx
    ON currencies (symbol);
CREATE INDEX IF NOT EXISTS currencies_replaced_by_idx
    ON currencies (replaced_by);
CREATE INDEX IF NOT EXISTS currencies_flag_id_idx
    ON currencies (flag_id);
CREATE INDEX IF NOT EXISTS currencies_name_trgm_idx
    ON currencies USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS currencies_native_name_trgm_idx
    ON currencies USING gin (lower(native_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS currency_translations_currency_language_idx
    ON currency_translations (currency_id, language_code);
CREATE INDEX IF NOT EXISTS currency_translations_language_name_idx
    ON currency_translations (language_code, name);
CREATE INDEX IF NOT EXISTS currency_translations_name_trgm_idx
    ON currency_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS countries_name_idx
    ON countries (name);
CREATE INDEX IF NOT EXISTS countries_phone_code_idx
    ON countries (phone_code);
CREATE INDEX IF NOT EXISTS countries_tld_idx
    ON countries (tld);
CREATE INDEX IF NOT EXISTS countries_region_id_idx
    ON countries (region_id);
CREATE INDEX IF NOT EXISTS countries_subregion_id_idx
    ON countries (subregion_id);
CREATE INDEX IF NOT EXISTS countries_flag_id_idx
    ON countries (flag_id);
CREATE INDEX IF NOT EXISTS countries_name_trgm_idx
    ON countries USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS countries_native_name_trgm_idx
    ON countries USING gin (lower(native_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS country_translations_country_language_idx
    ON country_translations (country_id, language_code);
CREATE INDEX IF NOT EXISTS country_translations_language_name_idx
    ON country_translations (language_code, name);
CREATE INDEX IF NOT EXISTS country_translations_name_trgm_idx
    ON country_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS states_country_id_idx
    ON states (country_id);
CREATE INDEX IF NOT EXISTS states_name_idx
    ON states (name);
CREATE INDEX IF NOT EXISTS states_state_code_idx
    ON states (state_code);
CREATE INDEX IF NOT EXISTS states_type_idx
    ON states (type);
CREATE INDEX IF NOT EXISTS states_name_trgm_idx
    ON states USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS state_translations_state_language_idx
    ON state_translations (state_id, language_code);
CREATE INDEX IF NOT EXISTS state_translations_language_name_idx
    ON state_translations (language_code, name);
CREATE INDEX IF NOT EXISTS state_translations_name_trgm_idx
    ON state_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS cities_state_id_idx
    ON cities (state_id);
CREATE INDEX IF NOT EXISTS cities_name_idx
    ON cities (name);
CREATE INDEX IF NOT EXISTS cities_name_trgm_idx
    ON cities USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS city_translations_city_language_idx
    ON city_translations (city_id, language_code);
CREATE INDEX IF NOT EXISTS city_translations_language_name_idx
    ON city_translations (language_code, name);
CREATE INDEX IF NOT EXISTS city_translations_name_trgm_idx
    ON city_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS languages_name_idx
    ON languages (name);
CREATE INDEX IF NOT EXISTS languages_flag_id_idx
    ON languages (flag_id);
CREATE INDEX IF NOT EXISTS languages_name_trgm_idx
    ON languages USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS languages_native_name_trgm_idx
    ON languages USING gin (lower(native_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS language_translations_language_language_code_idx
    ON language_translations (language_id, language_code);
CREATE INDEX IF NOT EXISTS language_translations_language_code_name_idx
    ON language_translations (language_code, name);
CREATE INDEX IF NOT EXISTS language_translations_name_trgm_idx
    ON language_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS central_banks_name_idx
    ON central_banks (name);
CREATE INDEX IF NOT EXISTS central_banks_establishment_year_idx
    ON central_banks (establishment_year);
CREATE INDEX IF NOT EXISTS central_banks_name_trgm_idx
    ON central_banks USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS central_banks_native_name_trgm_idx
    ON central_banks USING gin (lower(native_name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS central_bank_translations_bank_language_idx
    ON central_bank_translations (central_bank_id, language_code);
CREATE INDEX IF NOT EXISTS central_bank_translations_language_name_idx
    ON central_bank_translations (language_code, name);
CREATE INDEX IF NOT EXISTS central_bank_translations_name_trgm_idx
    ON central_bank_translations USING gin (lower(name) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS country_timezones_timezone_id_idx
    ON country_timezones (timezone_id);
CREATE INDEX IF NOT EXISTS country_currencies_currency_id_idx
    ON country_currencies (currency_id);
CREATE INDEX IF NOT EXISTS country_languages_language_id_idx
    ON country_languages (language_id);
CREATE INDEX IF NOT EXISTS country_languages_type_idx
    ON country_languages (type);
CREATE INDEX IF NOT EXISTS central_bank_issued_currencies_currency_id_idx
    ON central_bank_issued_currencies (currency_id);
CREATE INDEX IF NOT EXISTS central_bank_countries_country_id_idx
    ON central_bank_countries (country_id);

CREATE INDEX IF NOT EXISTS data_import_version_version_idx
    ON data_import_version (version);
CREATE UNIQUE INDEX IF NOT EXISTS clients_access_key_hash_idx
    ON clients (access_key_hash);
CREATE INDEX IF NOT EXISTS clients_name_idx
    ON clients (name);
CREATE INDEX IF NOT EXISTS clients_name_trgm_idx
    ON clients USING gin (lower(name) public.gin_trgm_ops);

CREATE UNIQUE INDEX IF NOT EXISTS region_translations_region_language_uk
    ON region_translations (region_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS subregion_translations_subregion_language_uk
    ON subregion_translations (subregion_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS timezone_translations_timezone_language_uk
    ON timezone_translations (timezone_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS currency_translations_currency_language_uk
    ON currency_translations (currency_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS country_translations_country_language_uk
    ON country_translations (country_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS state_translations_state_language_uk
    ON state_translations (state_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS city_translations_city_language_uk
    ON city_translations (city_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS language_translations_language_language_code_uk
    ON language_translations (language_id, language_code);

CREATE UNIQUE INDEX IF NOT EXISTS central_bank_translations_bank_language_uk
    ON central_bank_translations (central_bank_id, language_code);

