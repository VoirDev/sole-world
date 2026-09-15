package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class CountryRestIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `country resolves by id, alpha-2 and alpha-3`() {
        val byId = get("/v1/countries/1")
        val byAlpha2 = get("/v1/countries/FD")
        val byAlpha3 = get("/v1/countries/FRE")

        assertEquals("Freedonia", byId["name"].stringValue())
        assertEquals(byId["id"].intValue(), byAlpha2["id"].intValue())
        assertEquals(byId["id"].intValue(), byAlpha3["id"].intValue())
    }

    @Test
    fun `country identifier matching ignores case`() {
        assertEquals("Freedonia", get("/v1/countries/fd")["name"].stringValue())
    }

    @Test
    fun `country returns its own fields without includes`() {
        val country = get("/v1/countries/1")

        assertEquals("FRE", country["iso3"].stringValue())
        assertEquals("901", country["isoNumeric"].stringValue())
        assertEquals("+11", country["phoneCode"].stringValue())
        assertEquals(".fd", country["tld"].stringValue())
        assertEquals(45.1, country.at("/coordinates/latitude").doubleValue())

        // Relationships the caller did not ask for are absent, not null.
        assertTrue(country["region"] == null, "region should be absent")
        assertTrue(country["currencies"] == null, "currencies should be absent")
    }

    @Test
    fun `include embeds each allowed relationship`() {
        val country = get(
            "/v1/countries/1?include=region,subregion,flag,currencies,languages,timezones,centralBanks,states",
        )

        assertEquals("Test Europe", country.at("/region/name").stringValue())
        assertEquals("Test North", country.at("/subregion/name").stringValue())
        assertEquals("Freedonia flag", country.at("/flag/caption").stringValue())
        assertEquals(listOf("Freedonian Credit"), itemsField(country, "currencies", "name"))
        assertEquals(listOf("Freedonian"), itemsField(country, "languages", "name"))
        assertEquals(listOf("Freedonia Time"), itemsField(country, "timezones", "tzName"))
        assertEquals(listOf("Freedonian Reserve"), itemsField(country, "centralBanks", "name"))
        assertEquals(
            listOf("North Freedonia", "South Freedonia"),
            itemsField(country, "states", "name").sorted(),
        )
    }

    @Test
    fun `included records do not carry their own includes`() {
        val country = get("/v1/countries/1?include=subregion")

        // Depth is capped at one: the subregion arrives without its own region or countries.
        assertTrue(country.at("/subregion/region").isMissingNode)
        assertTrue(country.at("/subregion/countries").isMissingNode)
    }

    @Test
    fun `unknown include is rejected with the accepted values`() {
        val problem = assertProblem(rest("/v1/countries/1?include=citiez"), status = 400)

        assertTrue(problem["detail"].stringValue().contains("citiez"))

        val allowed = mutableListOf<String>()
        for (value in problem["allowed"]) {
            allowed += value.stringValue()
        }

        assertTrue(allowed.contains("region"), "allowed values should be listed: $allowed")
        assertFalse(allowed.contains("cities"), "cities is deliberately not includable on a country")
    }

    @Test
    fun `cities are not includable on a country`() {
        assertProblem(rest("/v1/countries/1?include=cities"), status = 400)
    }

    @Test
    fun `unknown country is a problem document`() {
        val problem = assertProblem(rest("/v1/countries/ZZZ"), status = 404)

        assertTrue(problem["detail"].stringValue().contains("ZZZ"))
    }

    @Test
    fun `listing pages through countries`() {
        val first = get("/v1/countries?page=0&size=1")
        val second = get("/v1/countries?page=1&size=1")

        assertEquals(2, first.at("/page/totalItems").intValue())
        assertEquals(2, first.at("/page/totalPages").intValue())
        assertEquals(1, first.at("/page/size").intValue())
        assertTrue(first.at("/page/hasNextPage").booleanValue())
        assertFalse(first.at("/page/hasPreviousPage").booleanValue())

        assertFalse(second.at("/page/hasNextPage").booleanValue())
        assertTrue(second.at("/page/hasPreviousPage").booleanValue())

        assertTrue(itemNames(first).first() != itemNames(second).first())
    }

    @Test
    fun `page beyond the end is empty rather than an error`() {
        val page = get("/v1/countries?page=99&size=50")

        assertEquals(0, page["items"].size())
        assertEquals(2, page.at("/page/totalItems").intValue())
    }

    @Test
    fun `page size beyond the documented maximum is rejected`() {
        assertProblem(rest("/v1/countries?size=500"), status = 400)
        assertProblem(rest("/v1/countries?size=0"), status = 400)
        assertProblem(rest("/v1/countries?page=-1"), status = 400)
    }

    @Test
    fun `search ranks matches and tolerates a typo`() {
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?query=Freedonia")))
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?query=Freedonai")))
        assertEquals(listOf("Sylvania"), itemNames(get("/v1/countries?query=SYL")))
    }

    @Test
    fun `search matches a name in another language`() {
        // The query is Russian while the response is rendered in English.
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?query=Фридония")))
    }

    @Test
    fun `filters narrow the listing and combine`() {
        assertEquals(2, get("/v1/countries?regionId=10").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/countries?regionId=11").at("/page/totalItems").intValue())
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?currencyId=101")))
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?languageId=201")))
        assertEquals(listOf("Freedonia"), itemNames(get("/v1/countries?timezoneId=301")))
        assertEquals(
            listOf("Freedonia"),
            itemNames(get("/v1/countries?regionId=10&subregionId=20")),
        )
    }

    @Test
    fun `sub-resources are paginated and filtered`() {
        assertEquals(2, get("/v1/countries/1/states").at("/page/totalItems").intValue())
        assertEquals(2, get("/v1/countries/1/cities").at("/page/totalItems").intValue())
        assertEquals(1, get("/v1/countries/1/currencies").at("/page/totalItems").intValue())
        assertEquals(1, get("/v1/countries/1/languages").at("/page/totalItems").intValue())
        assertEquals(1, get("/v1/countries/1/timezones").at("/page/totalItems").intValue())
        assertEquals(1, get("/v1/countries/1/central-banks").at("/page/totalItems").intValue())

        val filtered = get("/v1/countries/1/cities?query=South")
        assertEquals(1, filtered.at("/page/totalItems").intValue())
        assertEquals(listOf("South City"), itemNames(filtered))
    }

    @Test
    fun `sub-resource of an unknown country is a not found, not an empty page`() {
        assertProblem(rest("/v1/countries/ZZZ/states"), status = 404)
    }

    @Test
    fun `country with no relationships returns empty pages`() {
        assertEquals(0, get("/v1/countries/2/states").at("/page/totalItems").intValue())
        assertEquals(0, get("/v1/countries/2/currencies").at("/page/totalItems").intValue())
    }

    private fun itemsField(node: JsonNode, field: String, property: String): List<String> {
        val values = mutableListOf<String>()
        for (item in node[field]) {
            values += item[property].stringValue()
        }

        return values
    }
}
