package dev.voir.sole.world.api.integration

import org.springframework.jdbc.core.JdbcTemplate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Seeds the shared integration-test database with a compact, connected world graph. */
class IntegrationTestDataSeeder(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun seed() {
        synchronized(seedLock) {
            if (seeded) {
                return
            }

            seedClient()
            seedMediaAndFlags()
            seedRegions()
            seedCurrencies()
            seedCountries()
            seedLanguages()
            seedTimezones()
            seedCentralBanks()
            seedStatesAndCities()
            seedTranslations()

            seeded = true
        }
    }

    private fun seedClient() {
        jdbcTemplate.update(
            """
            INSERT INTO clients (name, description, access_key_hash, access_key_prefix, active)
            VALUES (?, ?, ?, ?, true)
            """.trimIndent(),
            "integration-test",
            "GraphQL integration tests",
            hmacSha256(ApiAccessKey.raw, ApiAccessKey.hashSecret),
            ApiAccessKey.raw.take(18),
        )
    }

    private fun seedMediaAndFlags() {
        jdbcTemplate.update(
            """
            INSERT INTO media_assets (id, type, image_aspect_ratio, image_formats, description)
            VALUES (?, 'image', 'square', ?::jsonb, ?)
            """.trimIndent(),
            9001L,
            """
            {
              "svg": "/assets/flags/fd_1x1.svg",
              "png": {"xs": "/fd_64.png", "sm": "/fd_128.png", "md": "/fd_256.png", "lg": "/fd_512.png", "xl": "/fd_1024.png"},
              "webp": {"xs": "/fd_64.webp", "sm": "/fd_128.webp", "md": "/fd_256.webp", "lg": "/fd_512.webp", "xl": "/fd_1024.webp"},
              "jpg": {"xs": "/fd_64.jpg", "sm": "/fd_128.jpg", "md": "/fd_256.jpg", "lg": "/fd_512.jpg", "xl": "/fd_1024.jpg"}
            }
            """.trimIndent(),
            "Freedonia square flag",
        )
        jdbcTemplate.update(
            """
            INSERT INTO media_assets (id, type, image_aspect_ratio, image_formats, description)
            VALUES (?, 'image', 'wide', ?::jsonb, ?)
            """.trimIndent(),
            9002L,
            """
            {
              "svg": "/assets/flags/fd_4x3.svg",
              "png": {"xs": "/fd_wide_64.png", "sm": "/fd_wide_128.png", "md": "/fd_wide_256.png", "lg": "/fd_wide_512.png", "xl": "/fd_wide_1024.png"},
              "webp": null,
              "jpg": null
            }
            """.trimIndent(),
            "Freedonia wide flag",
        )
        jdbcTemplate.update(
            """
            INSERT INTO flags (id, caption, emoji, emoji_u, square_asset_id, wide_asset_id)
            VALUES (1001, 'Freedonia flag', 'FD', 'U+1F1EB U+1F1E9', 9001, 9002)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO flags (id, caption, emoji, emoji_u, square_asset_id, wide_asset_id)
            VALUES (1002, 'Sylvania flag', 'SY', 'U+1F1F8 U+1F1FE', null, null)
            """.trimIndent(),
        )
    }

    private fun seedRegions() {
        jdbcTemplate.update("INSERT INTO regions (id, name, wiki_data_id) VALUES (10, 'Test Europe', 'Q10')")
        jdbcTemplate.update("INSERT INTO regions (id, name, wiki_data_id) VALUES (11, 'Test Oceania', 'Q11')")
        jdbcTemplate.update("INSERT INTO subregions (id, name, region_id, wiki_data_id) VALUES (20, 'Test North', 10, 'Q20')")
        jdbcTemplate.update("INSERT INTO subregions (id, name, region_id, wiki_data_id) VALUES (21, 'Test South', 10, 'Q21')")
    }

    private fun seedCurrencies() {
        jdbcTemplate.update(
            """
            INSERT INTO currencies (
              id, iso3, iso_numeric, name, description, native_name, symbol, year,
              introduced_date, obsolete, obsolete_at, replaced_by, flag_id, decimal_digits
            )
            VALUES (101, 'FDC', '901', 'Freedonian Credit', 'Active seeded currency', 'Credit', 'F$', 1991,
              '1991-01-01', false, null, null, 1001, 2)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO currencies (
              id, iso3, iso_numeric, name, description, native_name, symbol, year,
              introduced_date, obsolete, obsolete_at, replaced_by, flag_id, decimal_digits
            )
            VALUES (102, 'OLD', '902', 'Old Freedonian Credit', 'Obsolete seeded currency', 'Old Credit', 'O$', 1901,
              '1901-01-01', true, '1990-12-31', 101, 1001, 0)
            """.trimIndent(),
        )
    }

    private fun seedCountries() {
        jdbcTemplate.update(
            """
            INSERT INTO countries (
              id, name, native_name, iso3, iso2, iso_numeric, phone_code, tld,
              latitude, longitude, flag_id, region_id, subregion_id
            )
            VALUES (1, 'Freedonia', 'Freedonia Native', 'FRE', 'FD', '901', '+11', '.fd',
              45.1, 19.2, 1001, 10, 20)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO countries (
              id, name, native_name, iso3, iso2, iso_numeric, phone_code, tld,
              latitude, longitude, flag_id, region_id, subregion_id
            )
            VALUES (2, 'Sylvania', 'Sylvania Native', 'SYL', 'SY', '902', '+22', '.sy',
              46.1, 20.2, null, 10, 21)
            """.trimIndent(),
        )
        jdbcTemplate.update("INSERT INTO country_currencies (country_id, currency_id) VALUES (1, 101)")
    }

    private fun seedLanguages() {
        jdbcTemplate.update(
            """
            INSERT INTO languages (id, code, native_name, name, description, flag_id)
            VALUES (201, 'fd', 'Freedonian', 'Freedonian', 'Seeded official language', 1001)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO languages (id, code, native_name, name, description, flag_id)
            VALUES (202, 'sy', 'Sylvanian', 'Sylvanian', 'Seeded secondary language', 1002)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            "INSERT INTO country_languages (country_id, language_id, type) VALUES (1, 201, 'official')",
        )
    }

    private fun seedTimezones() {
        jdbcTemplate.update(
            """
            INSERT INTO timezones (id, zone_name, gmt_offset, gmt_offset_name, abbreviation, tz_name)
            VALUES (301, 'Europe/Freedonia', 3600, 'UTC+01:00', 'FDT', 'Freedonia Time')
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO timezones (id, zone_name, gmt_offset, gmt_offset_name, abbreviation, tz_name)
            VALUES (302, 'Europe/Sylvania', 7200, 'UTC+02:00', 'SYT', 'Sylvania Time')
            """.trimIndent(),
        )
        jdbcTemplate.update("INSERT INTO country_timezones (country_id, timezone_id) VALUES (1, 301)")
    }

    private fun seedCentralBanks() {
        jdbcTemplate.update(
            """
            INSERT INTO central_banks (id, name, native_name, website_url, establishment_year)
            VALUES (401, 'Freedonian Reserve', 'Reserve Native', 'https://bank.example.test', 1950)
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO central_banks (id, name, native_name, website_url, establishment_year)
            VALUES (402, 'Sylvania Reserve', 'Sylvania Reserve Native', 'https://syl-bank.example.test', 1960)
            """.trimIndent(),
        )
        jdbcTemplate.update("INSERT INTO central_bank_countries (central_bank_id, country_id) VALUES (401, 1)")
        jdbcTemplate.update("INSERT INTO central_bank_issued_currencies (central_bank_id, currency_id) VALUES (401, 101)")
    }

    private fun seedStatesAndCities() {
        jdbcTemplate.update(
            """
            INSERT INTO states (id, country_id, name, state_code, latitude, longitude, type)
            VALUES (501, 1, 'North Freedonia', 'NF', 45.2, 19.3, 'province')
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO states (id, country_id, name, state_code, latitude, longitude, type)
            VALUES (502, 1, 'South Freedonia', 'SF', null, null, 'province')
            """.trimIndent(),
        )
        jdbcTemplate.update("INSERT INTO cities (id, state_id, name, latitude, longitude) VALUES (601, 501, 'North City', 45.21, 19.31)")
        jdbcTemplate.update("INSERT INTO cities (id, state_id, name, latitude, longitude) VALUES (602, 502, 'South City', 44.98, 19.01)")
    }

    private fun seedTranslations() {
        jdbcTemplate.update("INSERT INTO region_translations (region_id, language_code, name) VALUES (10, 'ru', 'Тестовая Европа')")
        jdbcTemplate.update("INSERT INTO subregion_translations (subregion_id, language_code, name) VALUES (20, 'ru', 'Тестовый Север')")
        jdbcTemplate.update("INSERT INTO country_translations (country_id, language_code, name) VALUES (1, 'ru', 'Фридония')")
        jdbcTemplate.update(
            """
            INSERT INTO currency_translations (currency_id, language_code, name, description)
            VALUES (101, 'ru', 'Фридонский кредит', 'Активная тестовая валюта')
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO language_translations (language_id, language_code, name, description)
            VALUES (201, 'ru', 'Фридонский', 'Тестовый официальный язык')
            """.trimIndent(),
        )
        jdbcTemplate.update("INSERT INTO timezone_translations (timezone_id, language_code, tz_name) VALUES (301, 'ru', 'Фридонское время')")
        jdbcTemplate.update("INSERT INTO central_bank_translations (central_bank_id, language_code, name) VALUES (401, 'ru', 'Фридонский резерв')")
        jdbcTemplate.update("INSERT INTO state_translations (state_id, language_code, name) VALUES (501, 'ru', 'Северная Фридония')")
        jdbcTemplate.update("INSERT INTO state_translations (state_id, language_code, name) VALUES (502, 'ru', 'Южная Фридония')")
        jdbcTemplate.update("INSERT INTO city_translations (city_id, language_code, name) VALUES (601, 'ru', 'Северный город')")
        jdbcTemplate.update("INSERT INTO city_translations (city_id, language_code, name) VALUES (602, 'ru', 'Южный город')")
    }

    private fun hmacSha256(raw: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(raw.toByteArray(Charsets.UTF_8)).joinToString(separator = "") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    companion object {
        private val seedLock = Any()
        private var seeded = false
    }
}

object ApiAccessKey {
    const val raw = "sole_test_integration_key"
    const val hashSecret = "integration-test-secret"
}
