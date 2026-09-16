package dev.voir.sole.world.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Sets the response headers that hold across every route.
 *
 * This runs before authentication, so a `401` carries them too — a rejected request is still a
 * response a browser will act on.
 *
 * `nosniff` is the one that matters here. This service hands out JSON, SVG and RFC 9457 problem
 * documents, and some of those documents echo part of the request back: an unknown `include` value
 * is named in `detail` so the caller can see what they got wrong, and the request URI appears in
 * `instance`. None of that is a problem while the browser treats the response as what it says it is,
 * and `nosniff` is what makes that true whatever the bytes happen to look like.
 *
 * `Referrer-Policy` keeps an API key out of a `Referer` header. Keys travel in a header rather than
 * a query string, so this is belt and braces, but a caller who does put one in a URL should not have
 * it leak to wherever they navigate next.
 *
 * The two console pages add a `Content-Security-Policy` of their own; see `ConsoleAssets`. Nothing
 * else this service serves is a document a policy would apply to.
 */
@Component
@Order(FilterOrder.RESPONSE_HEADERS)
class SecurityHeadersFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.setHeader(CONTENT_TYPE_OPTIONS, "nosniff")
        response.setHeader(REFERRER_POLICY, "no-referrer")

        filterChain.doFilter(request, response)
    }

    private companion object {
        const val CONTENT_TYPE_OPTIONS = "X-Content-Type-Options"
        const val REFERRER_POLICY = "Referrer-Policy"
    }
}
