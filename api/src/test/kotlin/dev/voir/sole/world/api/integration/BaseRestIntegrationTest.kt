package dev.voir.sole.world.api.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import tools.jackson.databind.JsonNode
import java.net.http.HttpResponse

/** Base class for tests that exercise the REST transport. */
abstract class BaseRestIntegrationTest : BaseApiIntegrationTest() {
    /**
     * Sends a GET and asserts it succeeded.
     * @param path Path and query string.
     * @param acceptLanguage Value for the `Accept-Language` header.
     * @return Parsed response body.
     */
    protected fun get(path: String, acceptLanguage: String? = null): JsonNode {
        val response = rest(path, acceptLanguage = acceptLanguage)

        assertEquals(200, response.statusCode(), "GET $path returned ${response.body()}")
        return objectMapper.readTree(response.body())
    }

    /**
     * Sends a GET and returns the raw response, for status and header assertions.
     * @param path Path and query string.
     * @param apiKey Key to present, or null to send none.
     * @param acceptLanguage Value for the `Accept-Language` header.
     * @param ifNoneMatch Value for the `If-None-Match` header.
     * @return Raw HTTP response.
     */
    protected fun rest(
        path: String,
        apiKey: String? = ApiAccessKey.RAW,
        acceptLanguage: String? = null,
        ifNoneMatch: String? = null,
    ): HttpResponse<String> {
        val headers = buildMap {
            acceptLanguage?.let { put("Accept-Language", it) }
            ifNoneMatch?.let { put("If-None-Match", it) }
        }

        return send(path = path, apiKey = apiKey, headers = headers)
    }

    /**
     * Asserts a response is an RFC 9457 problem document with the expected status.
     * @param response Raw HTTP response.
     * @param status Expected status code.
     * @return Parsed problem document.
     */
    protected fun assertProblem(response: HttpResponse<String>, status: Int): JsonNode {
        assertEquals(status, response.statusCode(), response.body())
        assertEquals(
            "application/problem+json",
            response.headers().firstValue("Content-Type").orElse("").substringBefore(';'),
        )

        val problem = objectMapper.readTree(response.body())
        assertEquals(status, problem["status"].intValue())
        assertNotNull(problem["title"], "problem should carry a title")

        return problem
    }

    /** Reads the `name` of every item in a page response. */
    protected fun itemNames(page: JsonNode): List<String> {
        val names = mutableListOf<String>()
        for (item in page["items"]) {
            names += item["name"].stringValue()
        }

        return names
    }
}
