package dev.voir.sole.world.api.dataset

import dev.voir.sole.world.api.dataset.json.CentralBankJSON
import dev.voir.sole.world.api.dataset.json.CountryJSON
import dev.voir.sole.world.api.dataset.json.CryptoJSON
import dev.voir.sole.world.api.dataset.json.CurrencyJSON
import dev.voir.sole.world.api.dataset.json.FlagJSON
import dev.voir.sole.world.api.dataset.json.ImageFormatsJSON
import dev.voir.sole.world.api.dataset.json.LanguageJSON
import dev.voir.sole.world.api.dataset.json.LanguageTranslationJSON
import dev.voir.sole.world.api.dataset.json.MediaAssetJSON
import dev.voir.sole.world.api.dataset.json.RegionJSON
import dev.voir.sole.world.api.dataset.json.StateJSON
import dev.voir.sole.world.api.dataset.json.SubregionJSON
import dev.voir.sole.world.api.dataset.json.TimezoneJSON
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Covers the startup check that a dataset refers only to records it contains.
 *
 * A dangling reference used to surface at request time as a silently absent `include`, and a
 * duplicate id as a record the store's index quietly dropped — the bundled data shipped two
 * different languages under id 53, so one of them could not be loaded by id at all.
 */
class DatasetIntegrityTest {
    @Test
    fun `a consistent dataset passes`() {
        assertDoesNotThrow { DatasetIntegrity.check(dataset()) }
    }

    @Test
    fun `a duplicate id fails startup and names the collection`() {
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language("aa"), language("aa"))))
        }

        assertTrue(failure.message!!.contains("duplicate languages ids"), failure.message)
        assertTrue(failure.message!!.contains("aa"), failure.message)
    }

    @Test
    fun `ids that differ only in case are duplicates`() {
        // Lookups ignore case, so only one of the two could ever be found.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language("aa"), language("AA"))))
        }

        assertTrue(failure.message!!.contains("duplicate languages ids, ignoring case"), failure.message)
    }

    @Test
    fun `an id padded with whitespace fails startup`() {
        // Lookups trim what they are given, so a padded id could never be found.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language(" aa"))))
        }

        assertTrue(failure.message!!.contains("blank or padded languages ids"), failure.message)
    }

    @Test
    fun `a reference must spell the id the way the record does`() {
        // Lookups would find it either way, but the published data spells every id one way.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language("aa", flagId = "ONE"))))
        }

        assertTrue(failure.message!!.contains("language aa flagId=ONE"), failure.message)
    }

    @Test
    fun `a reference that does not resolve fails startup and names the row`() {
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language("aa", flagId = "missing"))))
        }

        assertTrue(failure.message!!.contains("flag references that do not resolve"), failure.message)
        assertTrue(failure.message!!.contains("language aa flagId=missing"), failure.message)
    }

    @Test
    fun `an absent reference is not a broken one`() {
        assertDoesNotThrow {
            DatasetIntegrity.check(dataset(languages = listOf(language("aa", flagId = null))))
        }
    }

    @Test
    fun `every problem is reported together`() {
        // One restart per broken row would make correcting a dataset needlessly slow.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(
                dataset(
                    languages = listOf(language("aa", flagId = "missing"), language("bb", flagId = "absent")),
                    cryptos = listOf(crypto("coin", logoId = "gone")),
                ),
            )
        }

        assertTrue(failure.message!!.contains("flagId=missing"), failure.message)
        assertTrue(failure.message!!.contains("flagId=absent"), failure.message)
        assertTrue(failure.message!!.contains("logoId=gone"), failure.message)
    }

    @Test
    fun `a flag naming an image of the wrong shape fails startup`() {
        // The id resolves and the response is well formed, so nothing downstream would notice that
        // the flag is serving a picture of the wrong shape.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(flags = listOf(flag(square = "one-wide", wide = "one-wide"))))
        }

        assertTrue(failure.message!!.contains("media assets of the wrong shape"), failure.message)
    }

    @Test
    fun `a flag whose two images are different pictures fails startup`() {
        // What caught Slovenia, India and Chile each naming the next flag's square image.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(flags = listOf(flag(square = "one-square", wide = "two-wide"))))
        }

        assertTrue(
            failure.message!!.contains("square and wide images are different pictures"),
            failure.message,
        )
        assertTrue(failure.message!!.contains("square='one' wide='two'"), failure.message)
    }

    @Test
    fun `a flag with only one rendition is not a mismatch`() {
        assertDoesNotThrow { DatasetIntegrity.check(dataset(flags = listOf(flag(wide = null)))) }
    }

    @Test
    fun `a translation in a language the dataset does not carry fails startup`() {
        // The locale never matches, so the caller is served base data for a name that was in fact
        // translated -- which reads as a gap in the translations rather than a broken row.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(
                dataset(languages = listOf(language("aa", translations = listOf("xx")))),
            )
        }

        assertTrue(
            failure.message!!.contains("translations in a language the dataset does not carry"),
            failure.message,
        )
        assertTrue(failure.message!!.contains("language aa languageCode='xx'"), failure.message)
    }

    @Test
    fun `a region qualified translation resolves through its primary subtag`() {
        // pt-BR is a legitimate tag for the Portuguese the dataset carries as pt.
        assertDoesNotThrow {
            DatasetIntegrity.check(
                dataset(languages = listOf(language("aa"), language("pt", translations = listOf("pt-BR")))),
            )
        }
    }

    @Test
    fun `a flag showing another territory's picture fails startup`() {
        // What caught Curaçao serving the Cape Verde flag: both renditions agreed with each other,
        // so only the emoji could say the picture belonged to a different country.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(
                dataset(flags = listOf(flag(emoji = "🇨🇼"))),
            )
        }

        assertTrue(
            failure.message!!.contains("flags showing another territory's picture"),
            failure.message,
        )
        assertTrue(failure.message!!.contains("expects 'cw' but shows 'one'"), failure.message)
    }

    @Test
    fun `a flag that is not a country flag has no territory to disagree with`() {
        // Subdivision and organisation flags are not a pair of regional indicators.
        assertDoesNotThrow { DatasetIntegrity.check(dataset(flags = listOf(flag(emoji = "🏴")))) }
    }

    @Test
    fun `a country reference into every related file is checked`() {
        for (country in listOf(
            country(regionId = "missing"),
            country(subregionId = "missing"),
            country(flagId = "missing"),
            country(currencyIds = listOf("missing")),
            country(timezoneIds = listOf("missing")),
            country(officialLanguageIds = listOf("missing")),
            country(otherLanguageIds = listOf("missing")),
        )) {
            assertThrows<IllegalStateException>("country with a dangling reference should fail") {
                DatasetIntegrity.check(dataset(countries = listOf(country)))
            }
        }
    }

    private fun dataset(
        languages: List<LanguageJSON> = listOf(language("aa")),
        cryptos: List<CryptoJSON> = listOf(crypto("coin")),
        countries: List<CountryJSON> = listOf(country()),
        centralBanks: List<CentralBankJSON> = emptyList(),
        flags: List<FlagJSON> = listOf(flag()),
    ) = RawDataset(
        meta = DatasetMeta(version = 1, date = "2026-01-01"),
        mediaAssets = listOf(
            mediaAsset("one-square", key = "one", aspectRatio = "square"),
            mediaAsset("one-wide", key = "one", aspectRatio = "wide"),
            mediaAsset("two-wide", key = "two", aspectRatio = "wide"),
        ),
        flags = flags,
        regions = listOf(
            RegionJSON(
                id = "region",
                name = "Region",
                wikiDataId = "Q30",
                translations = emptyList(),
                subregions = listOf(
                    SubregionJSON(
                        id = "subregion",
                        name = "Subregion",
                        wikiDataId = "Q40",
                        translations = emptyList(),
                    ),
                ),
            ),
        ),
        timezones = listOf(
            TimezoneJSON(
                id = "Test/Zone",
                zoneName = "Test/Zone",
                gmtOffset = 0,
                gmtOffsetName = "UTC",
                abbreviation = "TZ",
                tzName = "Test Zone",
                translations = emptyList(),
            ),
        ),
        currencies = listOf(
            CurrencyJSON(
                id = "TST",
                iso3 = "TST",
                isoNumeric = "001",
                name = "Test",
                description = null,
                nativeName = null,
                year = null,
                introducedDate = null,
                obsolete = false,
                obsoleteAt = null,
                replacedBy = null,
                symbol = null,
                flagId = "one",
                translations = emptyList(),
                decimalDigits = 2,
            ),
        ),
        cryptos = cryptos,
        languages = languages,
        countries = countries,
        centralBanks = centralBanks,
    )

    private fun flag(square: String? = "one-square", wide: String? = "one-wide", emoji: String = "F") =
        FlagJSON(id = "one", caption = "Flag", emoji = emoji, emojiU = "U", square = square, wide = wide)

    private fun mediaAsset(id: String, key: String = "test", aspectRatio: String = "square") = MediaAssetJSON(
        id = id,
        type = "image",
        key = key,
        imageAspectRatio = aspectRatio,
        imageFormats = ImageFormatsJSON(svg = null, png = null, webp = null, jpg = null),
        description = "asset",
    )

    private fun language(
        id: String,
        flagId: String? = "one",
        code: String = id,
        translations: List<String> = emptyList(),
    ) = LanguageJSON(
        id = id,
        code = code,
        nativeName = null,
        name = "Language $id",
        flagId = flagId,
        translations = translations.map { LanguageTranslationJSON(languageCode = it, name = "Name") },
    )

    private fun crypto(id: String, logoId: String? = "one-square") = CryptoJSON(
        id = id,
        code = id.uppercase(),
        name = "Test $id",
        description = null,
        websiteUrl = null,
        introducedYear = null,
        decimalDigits = 8,
        obsolete = false,
        obsoleteAt = null,
        logoId = logoId,
    )

    private fun country(
        regionId: String = "region",
        subregionId: String = "subregion",
        flagId: String? = "one",
        currencyIds: List<String> = listOf("TST"),
        timezoneIds: List<String> = listOf("Test/Zone"),
        officialLanguageIds: List<String> = listOf("aa"),
        otherLanguageIds: List<String> = emptyList(),
    ) = CountryJSON(
        id = "CO",
        name = "Country",
        nativeName = null,
        iso2 = "CO",
        iso3 = "CTY",
        numericCode = "001",
        phoneCode = "1",
        tld = null,
        latitude = 0.0,
        longitude = 0.0,
        flagId = flagId,
        regionId = regionId,
        subregionId = subregionId,
        currencyIds = currencyIds,
        timezoneIds = timezoneIds,
        officialLanguageIds = officialLanguageIds,
        otherLanguageIds = otherLanguageIds,
        translations = emptyList(),
        states = listOf(
            StateJSON(
                id = "CO-ST",
                name = "State",
                stateCode = null,
                latitude = null,
                longitude = null,
                type = null,
                translations = emptyList(),
                cities = emptyList(),
            ),
        ),
    )
}
