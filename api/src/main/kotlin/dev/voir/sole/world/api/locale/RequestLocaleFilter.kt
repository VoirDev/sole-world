package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.security.FilterOrder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Negotiates the translation locale for each data request and records it as [RequestLocale].
 *
 * REST honours an explicit `lang` query parameter over `Accept-Language`, which lets a caller pin a
 * language into the URL itself. That matters for caching: two languages then have two cache keys
 * instead of relying on every intermediary to honour `Vary`. GraphQL has only the header.
 *
 * A GraphQL response gets its `Content-Language` here, since every GraphQL response is rendered in
 * the negotiated locale. A REST response gets it from `ResponseCache`, alongside its validator, so
 * that a problem document — always English — does not claim to be in the requested language.
 */
@Component
@Order(FilterOrder.LOCALE)
class RequestLocaleFilter(localeStore: LocaleStore) : OncePerRequestFilter() {
    private val negotiation = localeStore.negotiation

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith(REST_PREFIX) && path != GRAPHQL
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val graphql = request.requestURI == GRAPHQL
        // Only REST has a `lang` parameter; GraphQL is not asked, so its body is never parsed here.
        val lang = if (graphql) null else request.getParameter(LANG_PARAMETER)
        val header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)

        val locale = if (lang.isNullOrBlank()) {
            negotiation.resolveHeader(header)
        } else {
            negotiation.resolveTag(lang)
        }
        RequestLocale.set(request, locale)

        if (graphql) {
            response.setHeader(HttpHeaders.CONTENT_LANGUAGE, RequestLocale.contentLanguage(locale))
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        const val REST_PREFIX = "/v1/"
        const val GRAPHQL = "/graphql"
        const val LANG_PARAMETER = "lang"
    }
}
