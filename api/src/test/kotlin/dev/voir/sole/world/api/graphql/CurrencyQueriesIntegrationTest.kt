package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CurrencyQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `currency query returns nested data`() {
        val response = graphQL(
            """
            query {
              currency(idOrCode: 101) {
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
    fun `currency query exposes introduced and obsolete dates`() {
        // These fields are declared in the schema and present in the dataset, but the previous
        // importer hard-coded both to null, so they were always absent from responses.
        val response = graphQL(
            """
            query {
              active: currency(idOrCode: 101) { introducedDate obsolete obsoleteAt }
              retired: currency(idOrCode: 102, withObsolete: true) {
                introducedDate obsolete obsoleteAt replacedById
                replacedBy { id iso3 }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)

        val active = response.at("/data/active")
        assertEquals("1991-01-01", active["introducedDate"].stringValue())
        assertFalse(active["obsolete"].booleanValue())
        assertTrue(isNullOrMissing(active["obsoleteAt"]))

        val retired = response.at("/data/retired")
        assertEquals("1901-01-01", retired["introducedDate"].stringValue())
        assertTrue(retired["obsolete"].booleanValue())
        assertEquals("1990-12-31", retired["obsoleteAt"].stringValue())
        assertEquals("101", retired["replacedById"].stringValue())
        assertEquals("FDC", retired.at("/replacedBy/iso3").stringValue())
    }

    @Test
    fun `currencies query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: currenciesByIds(ids: [101, 102, 999]) { id iso3 }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "iso3", "FDC", "OLD")
    }

    @Test
    fun `a currency resolves by id, alpha code and numeric code`() {
        val response = graphQL(
            """
            query {
              alpha: currency(idOrCode: "FDC") { id iso3 obsolete }
              numeric: currency(idOrCode: "901") { id iso3 obsolete }
              obsoleteHidden: currency(idOrCode: "OLD") { id }
              obsoleteVisible: currency(idOrCode: "OLD", withObsolete: true) { id iso3 obsolete }
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
              currency(idOrCode: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/currency")))
    }
}
