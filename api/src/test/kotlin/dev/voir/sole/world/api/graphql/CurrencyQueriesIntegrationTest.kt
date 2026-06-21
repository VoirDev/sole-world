package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CurrencyQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `currency query returns nested data`() {
        val response = graphQL(
            """
            query {
              currency(id: 101) {
                id
                iso3
                isoNumeric
                name
                decimalDigits
                obsolete
                symbol
                flag { id caption squareAsset { id type } }
                countries { id name }
                centralBanks { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val currency = response.at("/data/currency")
        assertEquals("101", currency["id"].stringValue())
        assertEquals("FDC", currency["iso3"].stringValue())
        assertEquals("901", currency["isoNumeric"].stringValue())
        assertEquals("Freedonian Credit", currency["name"].stringValue())
        assertEquals(2, currency["decimalDigits"].intValue())
        assertEquals("F$", currency["symbol"].stringValue())
        assertEquals("Freedonia flag", currency.at("/flag/caption").stringValue())
        assertArrayValues(currency["countries"], "name", "Freedonia")
        assertArrayValues(currency["centralBanks"], "name", "Freedonian Reserve")
    }

    @Test
    fun `currencies query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: currencies(ids: [101, 102, 999]) { id iso3 }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "iso3", "FDC", "OLD")
    }

    @Test
    fun `resolveCurrency supports alpha numeric and obsolete parameters`() {
        val response = graphQL(
            """
            query {
              alpha: resolveCurrency(identifier: "FDC") { id iso3 obsolete }
              numeric: resolveCurrency(identifier: "901") { id iso3 obsolete }
              obsoleteHidden: resolveCurrency(identifier: "OLD") { id }
              obsoleteVisible: resolveCurrency(identifier: "OLD", withObsolete: true) { id iso3 obsolete }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val data = response["data"]
        assertEquals("FDC", data.at("/alpha/iso3").stringValue())
        assertEquals("FDC", data.at("/numeric/iso3").stringValue())
        assertTrue(isNullOrMissing(data["obsoleteHidden"]))
        assertEquals("OLD", data.at("/obsoleteVisible/iso3").stringValue())
        assertTrue(data.at("/obsoleteVisible/obsolete").booleanValue())
    }

    @Test
    fun `missing currency query returns null`() {
        val response = graphQL(
            """
            query {
              currency(id: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/currency")))
    }
}
