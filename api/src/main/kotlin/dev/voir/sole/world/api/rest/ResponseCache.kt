package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.locale.RequestLocale
import dev.voir.sole.world.api.security.ApiKeyVerifier
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Duration

/**
 * Adds validators and caching headers to REST responses.
 *
 * The dataset is fixed for the life of a deployment, so a response is fully determined by the
 * request URL, the language that URL resolves to and the dataset version. That makes a strong `ETag`
 * exactly correct rather than merely a good guess — and, because none of those inputs require
 * touching a store, it makes the validator computable before the request is handled at all. That is
 * what [ConditionalRequestInterceptor] uses to answer a conditional request without building the
 * body.
 *
 * How widely a response may be cached follows the auth model rather than the content. The data is
 * not confidential, but on a deployment that requires a key, marking a response publicly cacheable
 * invites a shared cache to store a body fetched with a valid key and serve it to callers presenting
 * none — the key would stop being enforceable at the edge. `private` says what is actually true:
 * this response was fetched for one caller.
 */
@Component
class ResponseCache(dataset: RawDataset, verifier: ApiKeyVerifier) {
    private val datasetVersion = dataset.meta.version

    private val cacheControl: String = CacheControl
        .maxAge(Duration.ofHours(1))
        .let { if (verifier.enabled) it.cachePrivate() else it.cachePublic() }
        .staleWhileRevalidate(Duration.ofHours(24))
        .headerValue
        .orEmpty()

    /**
     * Wraps a payload with cache headers.
     *
     * A conditional request never reaches here: it is answered before the handler runs.
     *
     * @param body Response payload.
     * @param request Current HTTP request.
     * @return Response carrying the payload and its validator.
     */
    fun <T : Any> ok(body: T, request: HttpServletRequest): ResponseEntity<T> {
        return ResponseEntity
            .ok()
            .headers { applyHeaders(it, RestRequest.language(request)) }
            .eTag(etagFor(request))
            .body(body)
    }

    /**
     * Builds the validator for a request.
     *
     * The language is read from the request rather than taken from the handler, so that the
     * validator is a pure function of the request and can be computed on either side of it.
     *
     * @param request Current HTTP request.
     * @return Strong entity tag.
     */
    fun etagFor(request: HttpServletRequest): String {
        val language = RestRequest.language(request)

        val identity = buildString {
            append(datasetVersion)
            append(' ').append(request.requestURI)
            append(' ').append(request.queryString.orEmpty())
            append(' ').append(language.orEmpty())
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        val hex = digest
            .take(16)
            .joinToString(separator = "") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

        return "\"$hex\""
    }

    /**
     * Checks whether the caller already holds the current representation.
     * @param request Current HTTP request.
     * @param etag Validator for this request.
     * @return True when `If-None-Match` names this validator.
     */
    fun isCurrent(request: HttpServletRequest, etag: String): Boolean {
        val presented = request.getHeader(HttpHeaders.IF_NONE_MATCH) ?: return false
        return presented.split(',').any { it.trim() == etag }
    }

    /**
     * Applies the caching and language headers every REST response carries.
     * @param request Current HTTP request.
     * @param response Response being written directly, outside the handler chain.
     */
    fun applyHeaders(request: HttpServletRequest, response: HttpServletResponse) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl)
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
        response.setHeader(
            HttpHeaders.CONTENT_LANGUAGE,
            RequestLocale.contentLanguage(RestRequest.language(request)),
        )
    }

    private fun applyHeaders(headers: HttpHeaders, language: String?) {
        headers.cacheControl = cacheControl
        // The same URL renders differently per language, so shared caches must key on it.
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
        // An unsupported language is served base data rather than refused, so this is how a caller
        // tells a translation from a fallback.
        headers.set(HttpHeaders.CONTENT_LANGUAGE, RequestLocale.contentLanguage(language))
    }
}
