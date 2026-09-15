package dev.voir.sole.world.api.mediaasset

/**
 * Turns a dataset asset path into the form callers are given.
 *
 * The dataset stores paths as `assets/flags/zw_1x1.svg`, with no leading slash, which is the one
 * shape a client cannot use without knowing to repair it: joining it naively to a base gives
 * `http://hostassets/...`, and resolving it against `/v1/media-assets/1` gives `/v1/assets/...`.
 * Publishing `/assets/flags/zw_1x1.svg` is unambiguous wherever it is used.
 *
 * With a base URL configured the same path is published as an absolute URL against it, which is how
 * a deployment fronts its assets with a CDN without the API having to know anything else about it.
 *
 * @param baseUrl Origin to publish assets under, or empty to publish them rooted at this deployment.
 */
class AssetUrl(baseUrl: String) {
    private val prefix: String = baseUrl.trim().trimEnd('/')

    /**
     * Publishes one asset path.
     * @param path Raw path from the dataset, or null when the asset has no rendition of that kind.
     * @return Path or URL a caller can use as-is, or null when there was nothing to publish.
     */
    fun of(path: String?): String? {
        val clean = path?.trim()?.ifEmpty { null } ?: return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean
        }

        return prefix + "/" + clean.removePrefix("/")
    }
}
