package dev.voir.sole.world.api.console

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.time.Duration

/**
 * Serves the developer consoles' pages and their vendored third-party assets.
 *
 * Everything a console needs is inside the image: the bundles are pinned by version, verified
 * against a digest at build time, and served from this origin. Nothing is fetched from a CDN, so a
 * console runs no code this project did not choose and works on an air-gapped deployment.
 *
 * Console files are held under `console/` on the classpath rather than under the static resource
 * path, so that switching a console off leaves nothing behind for the static resource handler to
 * serve.
 */
object ConsoleAssets {
    /**
     * Serves a console's HTML page.
     * @param page File name under `console/`.
     * @return The page, with a policy that keeps it from loading anything off this origin.
     */
    fun page(page: String): ResponseEntity<Resource> {
        return ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_HTML)
            .header(CONTENT_SECURITY_POLICY, PAGE_POLICY)
            .cacheControl(CacheControl.noCache())
            .body(ClassPathResource("$CONSOLE_ROOT/$page"))
    }

    /**
     * Serves one vendored asset.
     *
     * The name is matched against the files this console actually ships rather than being resolved
     * as a path, so nothing outside the vendored set is reachable however the request is spelled.
     *
     * @param console Console directory under `console/assets/`.
     * @param file Requested file name.
     * @return The asset, or `404` when the console does not ship a file by that name.
     */
    fun asset(console: String, file: String): ResponseEntity<Resource> {
        val mediaType = MEDIA_TYPES[file.substringAfterLast('.', "")]
            ?: return ResponseEntity.notFound().build()

        val resource = ClassPathResource("$CONSOLE_ROOT/assets/$console/$file")
        if (!resource.exists() || file.contains('/') || file.contains('\\')) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity
            .ok()
            .contentType(mediaType)
            // Every asset is pinned to a version, so a cached copy can never be the wrong one.
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(resource)
    }

    private const val CONSOLE_ROOT = "console"

    private const val CONTENT_SECURITY_POLICY = "Content-Security-Policy"

    /**
     * What a console page is allowed to load: its own origin, and inline styles and scripts for the
     * few lines of setup each page carries. Both consoles render data from this API only.
     */
    private const val PAGE_POLICY =
        "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "worker-src 'self' blob:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'none'; " +
            "form-action 'none'"

    private val MEDIA_TYPES = mapOf(
        "js" to MediaType.parseMediaType("text/javascript"),
        "css" to MediaType.parseMediaType("text/css"),
        "map" to MediaType.APPLICATION_JSON,
    )
}
