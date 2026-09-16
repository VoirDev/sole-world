package dev.voir.sole.world.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Rejects requests that do not carry a configured API key.
 *
 * Every route is guarded unless [PublicPaths] says otherwise. Guarding centrally rather than per
 * resolver means a newly added query or endpoint is protected by default instead of being silently
 * public until someone remembers to annotate it.
 *
 * A verified caller's identity is recorded on the request for the filters behind this one, so
 * per-caller accounting never has to look at the key again.
 */
@Component
@Order(FilterOrder.AUTHENTICATION)
class ApiKeyFilter(
    private val verifier: ApiKeyVerifier,
    private val publicPaths: PublicPaths,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // CORS preflight carries no credentials by design.
        return request.method == HttpMethod.OPTIONS.name() || publicPaths.isPublic(request.requestURI)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val caller = verifier.identify(presentedKey(request))
        if (caller == null) {
            reject(response)
            return
        }

        request.setAttribute(CallerIdentity.ATTRIBUTE, caller)
        filterChain.doFilter(request, response)
    }

    /**
     * Reads the API key from either supported header.
     * @param request Incoming HTTP request.
     * @return Presented key, or null when neither header carries one.
     */
    private fun presentedKey(request: HttpServletRequest): String? {
        request.getHeader(API_KEY_HEADER)?.trim()?.ifEmpty { null }?.let { return it }

        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)?.trim() ?: return null
        if (!authorization.regionMatches(0, BEARER_PREFIX, 0, BEARER_PREFIX.length, ignoreCase = true)) {
            return null
        }

        return authorization.substring(BEARER_PREFIX.length).trim().ifEmpty { null }
    }

    private fun reject(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(UNAUTHORIZED_BODY)
    }

    private companion object {
        const val API_KEY_HEADER = "X-API-KEY"
        const val BEARER_PREFIX = "Bearer "

        /** Fixed RFC 9457 payload; holding no request data keeps it safe to emit verbatim. */
        val UNAUTHORIZED_BODY = """
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":"A valid API key is required. Send it as an X-API-KEY header or as Authorization: Bearer <key>."}
        """.trimIndent()
    }
}
