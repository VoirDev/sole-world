package dev.voir.sole.world.api.console

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Whether this deployment publishes the GraphiQL console.
 * @property enabled False removes `/graphiql` and its assets from the application.
 */
@ConfigurationProperties(prefix = "api.graphiql")
data class GraphiqlProperties(
    val enabled: Boolean = true,
)

/**
 * Publishes the GraphiQL console.
 *
 * This replaces the console the GraphQL starter ships, which resolves React, GraphiQL and their
 * dependencies from esm.sh every time the page loads. That console cannot open on a deployment
 * without outbound internet access, and it runs whatever the CDN happens to return. This one is
 * served from the image.
 *
 * Turning `api.graphiql.enabled` off removes this controller, and with it every route it serves.
 */
@Controller
@EnableConfigurationProperties(GraphiqlProperties::class)
@ConditionalOnProperty(
    prefix = "api.graphiql",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class GraphiqlController {
    /**
     * Serves the GraphiQL console.
     * @return The console page, as HTML.
     */
    @GetMapping("/graphiql", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun console(): ResponseEntity<Resource> = ConsoleAssets.page("graphiql.html")

    /**
     * Serves one of the console's vendored bundles.
     * @param file Requested file name.
     * @return The asset, or `404` when the console does not ship a file by that name.
     */
    @GetMapping("/graphiql/assets/{file}")
    @ResponseBody
    fun asset(@PathVariable file: String): ResponseEntity<Resource> =
        ConsoleAssets.asset("graphiql", file)
}
