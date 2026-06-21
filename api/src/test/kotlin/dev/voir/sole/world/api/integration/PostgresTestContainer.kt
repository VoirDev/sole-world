package dev.voir.sole.world.api.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/** Shared PostgreSQL Testcontainer used by Spring integration tests. */
object PostgresTestContainer {
    private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
        DockerImageName.parse("postgres:16-alpine"),
    )
        .withDatabaseName("sole_world_test")
        .withUsername("sole")
        .withPassword("sole")

    fun registerProperties(registry: DynamicPropertyRegistry) {
        start()

        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    private fun start() {
        if (!postgres.isRunning) {
            postgres.start()
        }
    }
}
