package dev.voir.sole.world.api.mediaasset

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Serves the bundled image assets from the filesystem.
 *
 * They used to be copied onto the classpath, which made the application jar 130 MB — 90 MB of which
 * was 9,773 images that do not change between releases — so a one-line code change rebuilt and
 * reshipped all of it. Keeping them beside the jar instead lets the image hold them in a layer of
 * their own, which is pulled once and then reused by every later release.
 *
 * The files are immutable for the life of a release, so they are cached for a year and served
 * without an API key: nothing about which flag someone fetched is worth authenticating.
 */
@Configuration
@EnableConfigurationProperties(AssetUrlProperties::class)
class StaticAssetConfig(private val properties: AssetUrlProperties) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val directory = Path.of(properties.directory).toAbsolutePath().normalize()

        if (!Files.isDirectory(directory)) {
            // Not fatal: a deployment fronting its assets with a CDN has no reason to carry them.
            log.warn(
                "No asset directory at {}. /assets/** will answer 404. " +
                    "Set api.assets.directory (ASSET_DIRECTORY) if they live elsewhere.",
                directory,
            )
        }

        registry
            .addResourceHandler("/assets/**")
            // Spring treats a location as a directory only when it ends in a separator.
            .addResourceLocations(directory.toUri().toString().trimEnd('/') + "/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .resourceChain(true)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(StaticAssetConfig::class.java)
    }
}
