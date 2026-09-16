package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Covers the resources beyond countries, including how each one resolves and what it embeds. */
class ResourceRestIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `currency resolves by id, alpha code and numeric code`() {
        assertEquals("Freedonian Credit", get("/v1/currencies/101")["name"].stringValue())
        assertEquals("Freedonian Credit", get("/v1/currencies/FDC")["name"].stringValue())
        assertEquals("Freedonian Credit", get("/v1/currencies/901")["name"].stringValue())
        assertEquals("Freedonian Credit", get("/v1/currencies/fdc")["name"].stringValue())
    }

    @Test
    fun `an obsolete currency is hidden unless asked for`() {
        assertProblem(rest("/v1/currencies/OLD"), status = 404)

        val obsolete = get("/v1/currencies/OLD?withObsolete=true")
        assertTrue(obsolete["obsolete"].booleanValue())
        assertEquals(101, obsolete["replacedById"].intValue())
    }

    @Test
    fun `currency dates are published`() {
        // Both were declared in the schema but always null before the dataset was read directly.
        val active = get("/v1/currencies/101")
        assertEquals("1991-01-01", active["introducedDate"].stringValue())

        val retired = get("/v1/currencies/OLD?withObsolete=true")
        assertEquals("1901-01-01", retired["introducedDate"].stringValue())
        assertEquals("1990-12-31", retired["obsoleteAt"].stringValue())
    }

    @Test
    fun `currency embeds its relationships`() {
        val currency = get("/v1/currencies/101?include=flag,countries,centralBanks")

        assertEquals("Freedonia flag", currency.at("/flag/caption").stringValue())
        assertEquals(listOf("Freedonia"), itemNames(wrap(currency, "countries")))
        assertEquals(listOf("Freedonian Reserve"), itemNames(wrap(currency, "centralBanks")))
    }

    @Test
    fun `currency can embed the currency that replaced it`() {
        val retired = get("/v1/currencies/OLD?withObsolete=true&include=replacedBy")

        assertEquals("Freedonian Credit", retired.at("/replacedBy/name").stringValue())
    }

    @Test
    fun `currencies can be filtered by obsolescence`() {
        assertEquals(
            listOf("Freedonian Credit", "Alpine Thaler"),
            itemNames(get("/v1/currencies?obsolete=false")),
        )
        assertEquals(listOf("Old Freedonian Credit"), itemNames(get("/v1/currencies?obsolete=true")))
        assertEquals(3, get("/v1/currencies").at("/page/totalItems").intValue())
    }

    @Test
    fun `a currency carries how widely it is used`() {
        assertEquals(60, get("/v1/currencies/FDC")["popularity"].intValue())
        // A currency nobody uses any more is not a currency anybody is looking for.
        assertEquals(0, get("/v1/currencies/OLD?withObsolete=true")["popularity"].intValue())
    }

    @Test
    fun `currencies are listed by popularity rather than by name`() {
        assertEquals(
            listOf("Freedonian Credit", "Alpine Thaler", "Old Freedonian Credit"),
            itemNames(get("/v1/currencies")),
        )
    }

    @Test
    fun `currencies can be sorted by any documented field, in either direction`() {
        assertEquals(
            listOf("Alpine Thaler", "Freedonian Credit", "Old Freedonian Credit"),
            itemNames(get("/v1/currencies?sort=name")),
        )
        assertEquals(
            listOf("Old Freedonian Credit", "Freedonian Credit", "Alpine Thaler"),
            itemNames(get("/v1/currencies?sort=name&order=desc")),
        )
        assertEquals(
            listOf("Alpine Thaler", "Freedonian Credit", "Old Freedonian Credit"),
            itemNames(get("/v1/currencies?sort=code")),
        )
        // Oldest first, and descending popularity is what the default already gives.
        assertEquals(
            listOf("Old Freedonian Credit", "Freedonian Credit", "Alpine Thaler"),
            itemNames(get("/v1/currencies?sort=year")),
        )
        assertEquals(
            listOf("Old Freedonian Credit", "Alpine Thaler", "Freedonian Credit"),
            itemNames(get("/v1/currencies?sort=popularity&order=asc")),
        )
    }

    @Test
    fun `an unknown sort or order names the values that exist`() {
        val sort = assertProblem(rest("/v1/currencies?sort=popularty"), status = 400)
        assertTrue(sort["detail"].stringValue().contains("popularty"))
        assertEquals("popularity", sort["allowed"][0].stringValue())

        val order = assertProblem(rest("/v1/currencies?order=sideways"), status = 400)
        val directions = mutableListOf<String>()
        for (direction in order["allowed"]) {
            directions += direction.stringValue()
        }
        assertEquals(listOf("asc", "desc"), directions)
    }

    @Test
    fun `an explicit sort overrides relevance ranking`() {
        assertEquals(
            listOf("Freedonian Credit", "Old Freedonian Credit"),
            itemNames(get("/v1/currencies?query=Credit")),
        )
        assertEquals(
            listOf("Old Freedonian Credit", "Freedonian Credit"),
            itemNames(get("/v1/currencies?query=Credit&sort=name&order=desc")),
        )
    }

    @Test
    fun `currency sub-resources resolve for an obsolete currency`() {
        // An obsolete currency still has countries and banks worth listing.
        assertEquals(200, rest("/v1/currencies/OLD/countries").statusCode())
        assertEquals(200, rest("/v1/currencies/OLD/central-banks").statusCode())
    }

    @Test
    fun `language resolves by id and code and embeds its countries`() {
        assertEquals("Freedonian", get("/v1/languages/201")["name"].stringValue())

        val byCode = get("/v1/languages/fd?include=countries,flag")
        assertEquals("Freedonian", byCode["name"].stringValue())
        assertEquals("Seeded official language", byCode["description"].stringValue())
        assertEquals(listOf("Freedonia"), itemNames(wrap(byCode, "countries")))
        assertEquals("Freedonia flag", byCode.at("/flag/caption").stringValue())
    }

    @Test
    fun `region embeds its subregions and countries`() {
        val region = get("/v1/regions/10?include=subregions,countries")

        assertEquals("Test Europe", region["name"].stringValue())
        assertEquals(listOf("Test North", "Test South"), itemNames(wrap(region, "subregions")).sorted())
        assertEquals(listOf("Freedonia", "Sylvania"), itemNames(wrap(region, "countries")).sorted())
    }

    @Test
    fun `region sub-resources are paginated`() {
        assertEquals(2, get("/v1/regions/10/subregions").at("/page/totalItems").intValue())
        assertEquals(2, get("/v1/regions/10/countries").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/regions/11/subregions").at("/page/totalItems").intValue())
    }

    @Test
    fun `subregion embeds its parent region and is filterable by region`() {
        val subregion = get("/v1/subregions/20?include=region,countries")

        assertEquals("Test North", subregion["name"].stringValue())
        assertEquals("Test Europe", subregion.at("/region/name").stringValue())
        assertEquals(listOf("Freedonia"), itemNames(wrap(subregion, "countries")))

        assertEquals(2, get("/v1/subregions?regionId=10").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/subregions?regionId=11").at("/page/totalItems").intValue())
    }

    @Test
    fun `central bank embeds its countries and currencies`() {
        val bank = get("/v1/central-banks/401?include=countries,currencies")

        assertEquals("Freedonian Reserve", bank["name"].stringValue())
        assertEquals("https://bank.example.test", bank["websiteUrl"].stringValue())
        assertEquals(1950, bank["establishmentYear"].intValue())
        assertEquals(listOf("Freedonia"), itemNames(wrap(bank, "countries")))
        assertEquals(listOf("Freedonian Credit"), itemNames(wrap(bank, "currencies")))
    }

    @Test
    fun `state embeds its country and cities and is filterable by country`() {
        val state = get("/v1/states/501?include=country,cities")

        assertEquals("North Freedonia", state["name"].stringValue())
        assertEquals("NF", state["stateCode"].stringValue())
        assertEquals("Freedonia", state.at("/country/name").stringValue())
        assertEquals(listOf("North City"), itemNames(wrap(state, "cities")))

        assertEquals(2, get("/v1/states?countryId=1").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/states?countryId=2").at("/page/totalItems").intValue())
    }

    @Test
    fun `a state without coordinates omits them`() {
        val state = get("/v1/states/502")

        assertTrue(state["coordinates"] == null, "absent coordinates should not be serialized")
    }

    @Test
    fun `city embeds its state and country`() {
        val city = get("/v1/cities/601?include=state,country")

        assertEquals("North City", city["name"].stringValue())
        assertEquals(501, city["stateId"].intValue())
        assertEquals("North Freedonia", city.at("/state/name").stringValue())
        assertEquals("Freedonia", city.at("/country/name").stringValue())
    }

    @Test
    fun `cities are filterable by country and state`() {
        assertEquals(2, get("/v1/cities?countryId=1").at("/page/totalItems").intValue())
        assertEquals(1, get("/v1/cities?stateId=501").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/cities?countryId=2").at("/page/totalItems").intValue())
    }

    @Test
    fun `state cities are paginated and filtered`() {
        assertEquals(1, get("/v1/states/501/cities").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/states/501/cities?query=South").at("/page/totalItems").intValue())
        assertProblem(rest("/v1/states/9999/cities"), status = 404)
    }

    @Test
    fun `timezone is served with its localized name`() {
        val timezone = get("/v1/timezones/301")

        assertEquals("Europe/Freedonia", timezone["zoneName"].stringValue())
        assertEquals("Freedonia Time", timezone["tzName"].stringValue())
        assertEquals(3600, timezone["gmtOffset"].intValue())

        assertEquals(
            "Фридонское время",
            get("/v1/timezones/301?lang=ru")["tzName"].stringValue(),
        )
    }

    @Test
    fun `flag embeds its media assets`() {
        val flag = get("/v1/flags/1001?include=squareAsset,wideAsset")

        assertEquals("Freedonia flag", flag["caption"].stringValue())
        assertEquals("image", flag.at("/squareAsset/type").stringValue())
        assertEquals("square", flag.at("/squareAsset/image/aspectRatio").stringValue())
        assertEquals("/assets/flags/fd_1x1.svg", flag.at("/squareAsset/image/formats/svg").stringValue())
        assertEquals("wide", flag.at("/wideAsset/image/aspectRatio").stringValue())
    }

    @Test
    fun `a flag without media assets omits them`() {
        val flag = get("/v1/flags/1002?include=squareAsset,wideAsset")

        assertTrue(flag["squareAsset"] == null)
        assertTrue(flag["wideAsset"] == null)
    }

    @Test
    fun `media asset exposes every image format it has`() {
        val asset = get("/v1/media-assets/9001")

        assertEquals("Freedonia square flag", asset["description"].stringValue())
        assertEquals("/fd_64.png", asset.at("/image/formats/png/xs").stringValue())
        assertEquals("/fd_1024.webp", asset.at("/image/formats/webp/xl").stringValue())

        // The wide asset has no webp or jpg renditions, and they are absent rather than null.
        val wide = get("/v1/media-assets/9002")
        assertTrue(wide.at("/image/formats/webp").isMissingNode)
    }

    @Test
    fun `unknown identifiers are not found across every resource`() {
        assertProblem(rest("/v1/currencies/999"), status = 404)
        assertProblem(rest("/v1/languages/999"), status = 404)
        assertProblem(rest("/v1/regions/999"), status = 404)
        assertProblem(rest("/v1/subregions/999"), status = 404)
        assertProblem(rest("/v1/central-banks/999"), status = 404)
        assertProblem(rest("/v1/states/999"), status = 404)
        assertProblem(rest("/v1/cities/999"), status = 404)
        assertProblem(rest("/v1/timezones/999"), status = 404)
        assertProblem(rest("/v1/flags/999"), status = 404)
        assertProblem(rest("/v1/media-assets/999"), status = 404)
    }

    @Test
    fun `each resource rejects an include it does not offer`() {
        assertProblem(rest("/v1/currencies/101?include=states"), status = 400)
        assertProblem(rest("/v1/languages/201?include=currencies"), status = 400)
        assertProblem(rest("/v1/regions/10?include=flag"), status = 400)
        assertProblem(rest("/v1/states/501?include=region"), status = 400)
        assertProblem(rest("/v1/cities/601?include=cities"), status = 400)
        assertProblem(rest("/v1/flags/1001?include=countries"), status = 400)
    }

    @Test
    fun `localized listings order by the name the caller sees`() {
        // Freedonia is translated and Sylvania is not, so the Latin name sorts before the Cyrillic
        // one. Ordering follows the rendered names, which is what keeps paging stable.
        assertEquals(
            listOf("Sylvania", "Фридония"),
            itemNames(get("/v1/countries?lang=ru&size=2")),
        )
    }

    @Test
    fun `search falls back to base names when a translation is missing`() {
        val page = get("/v1/countries?query=Sylvania&lang=ru")

        assertEquals(listOf("Sylvania"), itemNames(page))
        assertFalse(page["items"].isEmpty)
    }

    private fun wrap(node: tools.jackson.databind.JsonNode, field: String): tools.jackson.databind.JsonNode =
        objectMapper.createObjectNode().set("items", node[field])
}
