package dev.voir.sole.world.api.throttle

import dev.voir.sole.world.api.security.CallerIdentity
import dev.voir.sole.world.api.security.FilterOrder
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
 * Limits how fast one caller may ask for data.
 *
 * Only the routes that read the dataset are limited. Static assets are served by the servlet
 * container from the classpath and the health check must answer an orchestrator no matter how busy
 * the service is, so neither spends a caller's allowance.
 */
@Component
@Order(FilterOrder.RATE_LIMIT)
class RateLimitFilter(private val rateLimiter: RateLimiter) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!rateLimiter.enabled || request.method == HttpMethod.OPTIONS.name()) {
            return true
        }

        val path = request.requestURI
        return !path.startsWith(DATA_PREFIX) && path != GRAPHQL
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        when (val decision = rateLimiter.tryAcquire(CallerIdentity.of(request))) {
            is Allowed -> filterChain.doFilter(request, response)
            is Throttled -> reject(response, decision.retryAfterSeconds)
        }
    }

    private fun reject(response: HttpServletResponse, retryAfterSeconds: Long) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(body(retryAfterSeconds))
    }

    /** Holds no request data, so the document is safe to emit verbatim. */
    private fun body(retryAfterSeconds: Long): String =
        """{"type":"about:blank","title":"Too many requests","status":429,""" +
            """"detail":"Request rate exceeded. Retry after $retryAfterSeconds second(s)."}"""

    private companion object {
        const val DATA_PREFIX = "/v1/"
        const val GRAPHQL = "/graphql"
    }
}
