package dev.voir.sole.world.api.dataset

import dev.voir.sole.world.api.dataset.json.CentralBankJSON
import dev.voir.sole.world.api.dataset.json.CountryJSON
import dev.voir.sole.world.api.dataset.json.CryptoJSON
import dev.voir.sole.world.api.dataset.json.CurrencyJSON
import dev.voir.sole.world.api.dataset.json.FlagJSON
import dev.voir.sole.world.api.dataset.json.ImageFormatsJSON
import dev.voir.sole.world.api.dataset.json.LanguageJSON
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
            DatasetIntegrity.check(dataset(languages = listOf(language(1), language(1))))
        }

        assertTrue(failure.message!!.contains("duplicate languages ids"), failure.message)
        assertTrue(failure.message!!.contains("1"), failure.message)
    }

    @Test
    fun `a reference that does not resolve fails startup and names the row`() {
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(languages = listOf(language(1, flagId = 999))))
        }

        assertTrue(failure.message!!.contains("flag references that do not resolve"), failure.message)
        assertTrue(failure.message!!.contains("language 1 flagId=999"), failure.message)
    }

    @Test
    fun `an absent reference is not a broken one`() {
        assertDoesNotThrow {
            DatasetIntegrity.check(dataset(languages = listOf(language(1, flagId = null))))
        }
    }

    @Test
    fun `every problem is reported together`() {
        // One restart per broken row would make correcting a dataset needlessly slow.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(
                dataset(
                    languages = listOf(language(1, flagId = 999), language(2, flagId = 998)),
                    cryptos = listOf(crypto(1, logoId = 997)),
                ),
            )
        }

        assertTrue(failure.message!!.contains("flagId=999"), failure.message)
        assertTrue(failure.message!!.contains("flagId=998"), failure.message)
        assertTrue(failure.message!!.contains("logoId=997"), failure.message)
    }

    @Test
    fun `a flag naming an image of the wrong shape fails startup`() {
        // The id resolves and the response is well formed, so nothing downstream would notice that
        // the flag is serving a picture of the wrong shape.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(flags = listOf(flag(square = 11, wide = 11))))
        }

        assertTrue(failure.message!!.contains("media assets of the wrong shape"), failure.message)
    }

    @Test
    fun `a flag whose two images are different pictures fails startup`() {
        // What caught Slovenia, India and Chile each naming the next flag's square image.
        val failure = assertThrows<IllegalStateException> {
            DatasetIntegrity.check(dataset(flags = listOf(flag(square = 10, wide = 12))))
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
    fun `a country reference into every related file is checked`() {
        for (country in listOf(
            country(regionId = 999),
            country(subregionId = 999),
            country(flagId = 999),
            country(currencyIds = listOf(999)),
            country(timezoneIds = listOf(999)),
            country(officialLanguageIds = listOf(999)),
            country(otherLanguageIds = listOf(999)),
        )) {
            assertThrows<IllegalStateException>("country with a dangling reference should fail") {
                DatasetIntegrity.check(dataset(countries = listOf(country)))
            }
        }
    }

    private fun dataset(
        languages: List<LanguageJSON> = listOf(language(1)),
        cryptos: List<CryptoJSON> = listOf(crypto(1)),
        countries: List<CountryJSON> = listOf(country()),
        centralBanks: List<CentralBankJSON> = emptyList(),
        flags: List<FlagJSON> = listOf(flag()),
    ) = RawDataset(
        meta = DatasetMeta(version = 1, date = "2026-01-01"),
        mediaAssets = listOf(
            mediaAsset(10, key = "one", aspectRatio = "square"),
            mediaAsset(11, key = "one", aspectRatio = "wide"),
            mediaAsset(12, key = "two", aspectRatio = "wide"),
        ),
        flags = flags,
        regions = listOf(
            RegionJSON(
                id = 30,
                name = "Region",
                wikiDataId = "Q30",
                translations = emptyList(),
                subregions = listOf(
                    SubregionJSON(
                        id = 40,
                        name = "Subregion",
                        wikiDataId = "Q40",
                        translations = emptyList(),
                    ),
                ),
            ),
        ),
        timezones = listOf(
            TimezoneJSON(
                id = 50,
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
                id = 60,
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
                flagId = 20,
                translations = emptyList(),
                decimalDigits = 2,
            ),
        ),
        cryptos = cryptos,
        languages = languages,
        countries = countries,
        centralBanks = centralBanks,
    )

    private fun flag(square: Long? = 10, wide: Long? = 11) =
        FlagJSON(id = 20, caption = "Flag", emoji = "F", emojiU = "U", square = square, wide = wide)

    private fun mediaAsset(id: Long, key: String = "test", aspectRatio: String = "square") = MediaAssetJSON(
        id = id,
        type = "image",
        key = key,
        imageAspectRatio = aspectRatio,
        imageFormats = ImageFormatsJSON(svg = null, png = null, webp = null, jpg = null),
        description = "asset",
    )

    private fun language(id: Long, flagId: Long? = 20) =
        LanguageJSON(
            id = id,
            code = "t$id",
            nativeName = null,
            name = "Language $id",
            flagId = flagId,
            translations = emptyList(),
        )

    private fun crypto(id: Long, logoId: Long? = 10) = CryptoJSON(
        id = id,
        code = "t$id",
        alias = "test$id",
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
        regionId: Long = 30,
        subregionId: Long = 40,
        flagId: Long? = 20,
        currencyIds: List<Long> = listOf(60),
        timezoneIds: List<Long> = listOf(50),
        officialLanguageIds: List<Long> = listOf(1),
        otherLanguageIds: List<Long> = emptyList(),
    ) = CountryJSON(
        id = 70,
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
                id = 80,
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
