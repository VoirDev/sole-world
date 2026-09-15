package dev.voir.sole.world.api.configs

import dev.voir.sole.world.api.graphql.FieldCostCalculator
import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.Instrumentation
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/** Configures GraphQL query limits that abort oversized documents before resolver execution. */
@Configuration
@EnableConfigurationProperties(GraphqlLimitsProperties::class)
class GraphqlLimitsConfig(
    private val limits: GraphqlLimitsProperties,
) {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun maxQueryDepthInstrumentation(): Instrumentation {
        return MaxQueryDepthInstrumentation(limits.maxDepth)
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    fun maxQueryComplexityInstrumentation(): Instrumentation {
        return MaxQueryComplexityInstrumentation(limits.maxComplexity, FieldCostCalculator())
    }
}

/**
 * GraphQL document limits.
 * @property maxDepth Deepest selection path a document may contain.
 * @property maxComplexity Largest total field cost a document may reach; see `@cost` in the schema.
 */
@ConfigurationProperties(prefix = "graphql.limits")
data class GraphqlLimitsProperties(
    val maxDepth: Int = 30,
    val maxComplexity: Int = 1000,
)
