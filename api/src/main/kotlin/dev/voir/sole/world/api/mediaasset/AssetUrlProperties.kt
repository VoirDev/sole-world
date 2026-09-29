package dev.voir.sole.world.api.mediaasset

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * How asset paths are published to callers.
 *
 * @property baseUrl Origin asset paths are published under. Empty publishes them under `/assets/` on
 * this deployment, which is what a caller reading the API over HTTP wants. Set it to a CDN origin
 * that holds `flags/` and `cryptos/` at its root to publish absolute URLs pointing there instead.
 * @property directory Directory the `/assets/` route is served from, relative to the working
 * directory. The files sit beside the jar rather than inside it, so that 90 MB of images that never
 * change is not rebuilt and reshipped with every code change. Only read while [baseUrl] is empty,
 * since otherwise nothing published points at this deployment's copy.
 */
@ConfigurationProperties(prefix = "api.assets")
data class AssetUrlProperties(
    val baseUrl: String = "",
    val directory: String = "assets",
)
