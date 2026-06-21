package dev.voir.sole.world.api.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "admin.security.username=admin",
        "admin.security.password=admin",
        "client.security.access-key-hash-secret=integration-test-secret",
        "seed.import-enabled=false",
        "graphql.limits.max-depth=8",
        "graphql.limits.max-complexity=250",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseGraphqlIntegrationTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @LocalServerPort
    private var port: Int = 0

    private val httpClient = HttpClient.newHttpClient()

    @BeforeAll
    fun seedIntegrationDatabase() {
        IntegrationTestDataSeeder(jdbcTemplate).seed()
    }

    protected fun graphQL(
        query: String,
        apiKey: String? = ApiAccessKey.raw,
        acceptLanguage: String? = null,
    ): JsonNode {
        val body = objectMapper.writeValueAsString(mapOf("query" to query))
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/graphql"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))

        apiKey?.let { requestBuilder.header("X-API-KEY", it) }
        acceptLanguage?.let { requestBuilder.header("Accept-Language", it) }

        val response = httpClient.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        assertTrue(response.statusCode() in 200..299)
        return objectMapper.readTree(response.body())
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

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.registerProperties(registry)
        }
    }
}
