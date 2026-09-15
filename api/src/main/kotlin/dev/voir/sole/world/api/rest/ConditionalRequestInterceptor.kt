package dev.voir.sole.world.api.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Answers a conditional request before the handler runs.
 *
 * A validator computed after the payload is built saves serialization and bandwidth but none of the
 * work, which made a `304` on a large collection cost as much as the `200` it replaced. Every input
 * to the validator — dataset version, URL, query string, resolved language — is known before any
 * store is touched, so the answer belongs here: a caller whose copy is current pays for a hash.
 */
@Component
class ConditionalRequestInterceptor(private val cache: ResponseCache) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method != HttpMethod.GET.name()) {
            return true
        }

        val etag = cache.etagFor(request)
        if (!cache.isCurrent(request, etag)) {
            return true
        }

        response.status = HttpStatus.NOT_MODIFIED.value()
        response.setHeader(HttpHeaders.ETAG, etag)
        cache.applyHeaders(response)

        return false
    }
}

/**
 * Applies conditional handling to the data endpoints.
 *
 * Only the `/v1` tree is registered: those are the routes whose responses carry a validator. The
 * consoles and static assets have their own caching, and neither is worth answering conditionally
 * here.
 */
@Component
class ConditionalRequestConfig(
    private val interceptor: ConditionalRequestInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor).addPathPatterns("/v1/**")
    }
}
