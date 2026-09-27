package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanguageQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `language query returns nested data`() {
        val response = graphQL(
            """
            query {
              language(id: "fd") {
                id
                code
                nativeName
                name
                description
                flag { id caption }
                countries { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val language = response.at("/data/language")
        assertEquals("fd", language["id"].stringValue())
        assertEquals("fd", language["code"].stringValue())
        assertEquals("Freedonian", language["name"].stringValue())
        assertEquals("Freedonia flag", language.at("/flag/caption").stringValue())
        assertArrayValues(language["countries"], "name", "Freedonia")
    }

    @Test
    fun `languages query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: languagesByIds(ids: ["fd", "xx"]) { id code name }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "code", "fd")
    }

    @Test
    fun `missing language query returns null`() {
        val response = graphQL(
            """
            query {
              language(id: "xx") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/language")))
    }
}
