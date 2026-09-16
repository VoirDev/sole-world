package dev.voir.sole.world.api.configs

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Applies the configured CORS policy to every route.
 *
 * Credentials are never allowed: this API authenticates with an explicit header, so browsers have no
 * reason to attach cookies to it.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(
    private val cors: CorsProperties,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOriginPatterns(*cors.allowedOrigins.toTypedArray())
            .allowedMethods(*cors.allowedMethods.ifEmpty { DEFAULT_METHODS }.toTypedArray())
            .allowedHeaders(*cors.allowedHeaders.ifEmpty { DEFAULT_HEADERS }.toTypedArray())
            .allowCredentials(false)
            .maxAge(PREFLIGHT_MAX_AGE_SECONDS)
    }

    private companion object {
        val DEFAULT_METHODS = listOf("GET", "HEAD", "POST", "OPTIONS")
        val DEFAULT_HEADERS = listOf("*")
        const val PREFLIGHT_MAX_AGE_SECONDS = 3600L
    }
}
