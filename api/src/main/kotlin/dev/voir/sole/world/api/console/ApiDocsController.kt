package dev.voir.sole.world.api.console

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.time.Duration

/**
 * Whether this deployment publishes its OpenAPI contract and reference page.
 * @property enabled False removes `/openapi.yaml`, `/docs` and its assets from the application.
 */
@ConfigurationProperties(prefix = "api.docs")
data class ApiDocsProperties(
    val enabled: Boolean = true,
)

/**
 * Publishes the OpenAPI contract and its reference page.
 *
 * Both are served without an API key: a caller cannot reasonably be asked to authenticate before
 * they are allowed to read what the API offers. The document served here is the same file the
 * request and response types are generated from, so what is published cannot drift from what is
 * implemented.
 *
 * Turning `api.docs.enabled` off removes this controller, and with it every route it serves.
 */
@Controller
@EnableConfigurationProperties(ApiDocsProperties::class)
@ConditionalOnProperty(prefix = "api.docs", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ApiDocsController {
    /**
     * Serves the OpenAPI document.
     * @return The contract, as YAML.
     */
    @GetMapping("/openapi.yaml", produces = [OPENAPI_MEDIA_TYPE])
    @ResponseBody
    fun spec(): ResponseEntity<Resource> {
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(OPENAPI_MEDIA_TYPE))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(ClassPathResource(SPEC_PATH))
    }

    /**
     * Serves the API reference page.
     * @return The reference page, as HTML.
     */
    @GetMapping("/docs", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun docs(): ResponseEntity<Resource> = ConsoleAssets.page("docs.html")

    /**
     * Serves the reference page's vendored renderer.
     * @param file Requested file name.
     * @return The asset, or `404` when the page does not ship a file by that name.
     */
    @GetMapping("/docs/assets/{file}")
    @ResponseBody
    fun asset(@PathVariable file: String): ResponseEntity<Resource> = ConsoleAssets.asset("docs", file)

    private companion object {
        const val SPEC_PATH = "openapi/openapi.yaml"
        const val OPENAPI_MEDIA_TYPE = "application/yaml"
    }
}
