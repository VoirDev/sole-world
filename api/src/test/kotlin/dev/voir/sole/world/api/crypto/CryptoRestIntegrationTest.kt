package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Covers the cryptocurrency resource on the REST transport. */
class CryptoRestIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `a coin resolves by its alias id and by its ticker, in any case`() {
        assertEquals("Freecoin", get("/v1/cryptos/freecoin")["name"].stringValue())
        assertEquals("Freecoin", get("/v1/cryptos/FREECOIN")["name"].stringValue())
        assertEquals("Freecoin", get("/v1/cryptos/frc")["name"].stringValue())
        assertEquals("Freecoin", get("/v1/cryptos/FRC")["name"].stringValue())
    }

    @Test
    fun `an unknown identifier is a problem document`() {
        assertProblem(rest("/v1/cryptos/nosuchcoin"), status = 404)
    }

    @Test
    fun `every published field is carried`() {
        val coin = get("/v1/cryptos/frc")

        assertEquals("freecoin", coin["id"].stringValue())
        assertEquals("frc", coin["code"].stringValue())
        assertNull(coin["alias"], "the alias is the id, not a field of its own")
        assertEquals("The coin of Freedonia.", coin["description"].stringValue())
        assertEquals("https://freecoin.example", coin["websiteUrl"].stringValue())
        assertEquals(2011, coin["introducedYear"].intValue())
        assertEquals(8, coin["decimalDigits"].intValue())
        assertEquals("flag-fd-square", coin["logoId"].stringValue())
        assertEquals(false, coin["obsolete"].booleanValue())
    }

    @Test
    fun `a coin embeds the logo it points at`() {
        val coin = get("/v1/cryptos/frc?include=logo")

        assertEquals("flag-fd-square", coin.at("/logo/id").stringValue())
        assertEquals("/assets/flags/fd_1x1.svg", coin.at("/logo/image/formats/svg").stringValue())
    }

    @Test
    fun `a coin without a logo simply has none`() {
        val coin = get("/v1/cryptos/syl?include=logo")

        // Relationships a caller did not get are absent rather than null, on every resource.
        assertNull(coin["logoId"], "logoId should be absent, not null")
        assertNull(coin["logo"], "logo should be absent, not null")
    }

    @Test
    fun `an unknown include names the ones that exist`() {
        val problem = assertProblem(rest("/v1/cryptos/frc?include=bogus"), status = 400)

        assertTrue(problem["detail"].stringValue().contains("bogus"))
        assertEquals("logo", problem["allowed"][0].stringValue())
    }

    @Test
    fun `coins are listed by name`() {
        assertEquals(listOf("Freecoin", "Oldcoin", "Sylvacoin"), itemNames(get("/v1/cryptos")))
    }

    @Test
    fun `coins can be filtered by obsolescence`() {
        assertEquals(listOf("Freecoin", "Sylvacoin"), itemNames(get("/v1/cryptos?obsolete=false")))
        assertEquals(listOf("Oldcoin"), itemNames(get("/v1/cryptos?obsolete=true")))
    }

    @Test
    fun `an obsolete coin carries the date it wound down`() {
        assertEquals("2015-06-30", get("/v1/cryptos/old")["obsoleteAt"].stringValue())
    }

    @Test
    fun `search ranks over names, tickers and aliases`() {
        assertEquals(listOf("Freecoin"), itemNames(get("/v1/cryptos?query=Freecoin")))
        assertEquals(listOf("Freecoin"), itemNames(get("/v1/cryptos?query=frc")))
        assertEquals(listOf("Sylvacoin"), itemNames(get("/v1/cryptos?query=sylvacoin")))

        // Typo tolerance, the same as every other searchable resource.
        assertEquals(listOf("Sylvacoin"), itemNames(get("/v1/cryptos?query=Sylvacon")))
    }

    @Test
    fun `meta counts the coins this deployment serves`() {
        assertEquals(3, get("/v1/meta").at("/counts/cryptos").intValue())
    }
}
