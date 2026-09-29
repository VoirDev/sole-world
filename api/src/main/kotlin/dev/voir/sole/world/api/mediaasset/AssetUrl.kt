package dev.voir.sole.world.api.mediaasset

/**
 * Turns a dataset asset path into the form callers are given.
 *
 * The dataset stores each path relative to the asset root — `flags/zw_1x1.svg` — which is also its
 * key in the bucket a CDN serves from. That is the one shape a client cannot use as-is: joining it
 * naively to a base gives `http://hostflags/...`, and resolving it against `/v1/media-assets/1`
 * gives `/v1/media-assets/flags/...`.
 *
 * Served by this deployment, it is published under the route that serves the files, as
 * `/assets/flags/zw_1x1.svg`. With a base URL configured it is published as an absolute URL against
 * it, `https://cdn.example.com/flags/zw_1x1.svg`, because the CDN holds the folders at its root.
 *
 * @param baseUrl Origin to publish assets under, or empty to publish them rooted at this deployment.
 */
class AssetUrl(baseUrl: String) {
    private val prefix: String = baseUrl.trim().trimEnd('/').ifEmpty { ROUTE }

    /**
     * Publishes one asset path.
     * @param path Path from the dataset, relative to the asset root, or null when the asset has no
     * rendition of that kind.
     * @return Path or URL a caller can use as-is, or null when there was nothing to publish.
     */
    fun of(path: String?): String? {
        val clean = path?.trim()?.ifEmpty { null } ?: return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean
        }

        return prefix + "/" + clean.removePrefix("/")
    }

    companion object {
        /** Route this deployment serves the asset root under. */
        const val ROUTE = "/assets"
    }
}
