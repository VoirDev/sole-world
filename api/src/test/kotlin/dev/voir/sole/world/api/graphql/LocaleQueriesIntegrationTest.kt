package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocaleQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `locales query lists what accept language can select`() {
        val response = graphQL(
            """
            query {
              locales {
                items { id languageId name nativeName language { id name } }
                pageInfo { totalItems }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertEquals(1, response.at("/data/locales/pageInfo/totalItems").intValue())
        val locale = response.at("/data/locales/items/0")
        assertEquals("ru", locale["id"].stringValue())
        assertEquals("Russian", locale["name"].stringValue())
        assertEquals("Русский", locale["nativeName"].stringValue())
        assertEquals("Russian", locale.at("/language/name").stringValue())
    }

    @Test
    fun `locale query resolves a tag in any case and localizes nested fields`() {
        val response = graphQL(
            """
            query {
              locale(id: "RU") { id name language { id } }
              requested: localesByIds(ids: ["ru", "sv"]) { id }
            }
            """.trimIndent(),
            acceptLanguage = "ru",
        )

        assertNoErrors(response)
        assertEquals("ru", response.at("/data/locale/id").stringValue())
        assertEquals("Русский", response.at("/data/locale/name").stringValue())
        assertEquals("ru", response.at("/data/locale/language/id").stringValue())
        assertArrayValues(response.at("/data/requested"), "id", "ru")
    }

    @Test
    fun `a graphql response names the locale it is written in`() {
        val query = "query { country(id: \"FD\") { name } }"

        val russian = graphQLResponse(query, acceptLanguage = "ru-RU")
        assertEquals("ru", russian.headers().firstValue("Content-Language").orElse(null))
        assertTrue(russian.body().contains("Фридония"), russian.body())

        val fallback = graphQLResponse(query, acceptLanguage = "sv")
        assertEquals("en", fallback.headers().firstValue("Content-Language").orElse(null))
    }
}
