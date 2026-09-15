package dev.voir.sole.world.api.security

import dev.voir.sole.world.api.console.ApiDocsProperties
import dev.voir.sole.world.api.console.GraphiqlProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

/**
 * The routes served without an API key.
 *
 * The two developer consoles are only public while they are switched on. With a console disabled its
 * paths leave the allowlist as well as the application, so an unauthenticated caller learns nothing
 * about whether the deployment could have served it.
 *
 * @param docs Whether the OpenAPI contract and reference page are published.
 * @param graphiql Whether the GraphiQL console is published.
 */
@Component
@EnableConfigurationProperties(ApiDocsProperties::class, GraphiqlProperties::class)
class PublicPaths(
    docs: ApiDocsProperties,
    graphiql: GraphiqlProperties,
) {
    private val docsEnabled = docs.enabled

    private val graphiqlEnabled = graphiql.enabled

    /**
     * Checks whether a request path is reachable without credentials.
     * @param path Request path, without the query string.
     * @return True when the path is part of the public surface.
     */
    fun isPublic(path: String): Boolean {
        return when {
            path == HEALTH || path == ERROR -> true
            path.startsWith(ASSETS_PREFIX) -> true
            path == OPENAPI || path == DOCS || path.startsWith(DOCS_PREFIX) -> docsEnabled
            path == GRAPHIQL || path.startsWith(GRAPHIQL_PREFIX) -> graphiqlEnabled
            else -> false
        }
    }

    private companion object {
        const val HEALTH = "/healthz"

        /** Spring's internal error dispatch, which must reach the handler that renders the problem. */
        const val ERROR = "/error"
        const val ASSETS_PREFIX = "/assets/"
        const val OPENAPI = "/openapi.yaml"
        const val DOCS = "/docs"
        const val DOCS_PREFIX = "/docs/"
        const val GRAPHIQL = "/graphiql"
        const val GRAPHIQL_PREFIX = "/graphiql/"
    }
}
