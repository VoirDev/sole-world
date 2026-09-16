package dev.voir.sole.world.api.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import tools.jackson.databind.JsonNode
import java.net.http.HttpResponse

/** Base class for tests that exercise the GraphQL transport. */
abstract class BaseGraphqlIntegrationTest : BaseApiIntegrationTest() {
    protected fun graphQL(
        query: String,
        apiKey: String? = ApiAccessKey.RAW,
        acceptLanguage: String? = null,
    ): JsonNode {
        val response = graphQLResponse(query, apiKey, acceptLanguage)

        assertTrue(response.statusCode() in 200..299, "Unexpected status ${response.statusCode()}")
        return objectMapper.readTree(response.body())
    }

    /** Sends a GraphQL request and returns the raw HTTP response, for transport-level assertions. */
    protected fun graphQLResponse(
        query: String,
        apiKey: String? = ApiAccessKey.RAW,
        acceptLanguage: String? = null,
        bearerToken: String? = null,
    ): HttpResponse<String> {
        val headers = buildMap {
            bearerToken?.let { put("Authorization", "Bearer $it") }
            acceptLanguage?.let { put("Accept-Language", it) }
        }

        return send(
            path = "/graphql",
            apiKey = apiKey,
            headers = headers,
            body = objectMapper.writeValueAsString(mapOf("query" to query)),
        )
    }

    protected fun assertNoErrors(response: JsonNode) {
        assertNull(response["errors"], response.toPrettyString())
    }

    protected fun assertArrayValues(array: JsonNode, field: String, vararg expected: String) {
        assertNotNull(array, "Expected an array at $field")

        val actual = mutableListOf<String>()
        for (item in array) {
            actual += item[field].stringValue()
        }

        assertEquals(expected.toList().sorted(), actual.sorted())
    }

    protected fun assertArrayValues(array: JsonNode, field: String, vararg expected: Int) {
        assertNotNull(array, "Expected an array at $field")

        val actual = mutableListOf<Int>()
        for (item in array) {
            actual += item[field].intValue()
        }

        assertEquals(expected.toList().sorted(), actual.sorted())
    }

    protected fun isNullOrMissing(node: JsonNode?): Boolean {
        return node == null || node.isNull || node.isMissingNode
    }
}
