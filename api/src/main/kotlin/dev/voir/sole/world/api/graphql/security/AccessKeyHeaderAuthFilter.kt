package dev.voir.sole.world.api.graphql.security

import dev.voir.sole.world.api.database.ClientRepository
import dev.voir.sole.world.api.service.AccessKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/** Reads client API keys from headers and stores the result in the Spring security context. */
@Component
class AccessKeyHeaderAuthFilter(
    private val clientRepository: ClientRepository,
    private val accessKeyService: AccessKeyService,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI != "/graphql"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val provided = request.getHeader("X-API-KEY")?.trim()

        SecurityContextHolder.getContext().authentication = when {
            provided.isNullOrBlank() -> FailedAccessKeyAuthentication(AccessKeyFailureReason.Missing)
            clientRepository.isValidAccessKeyHash(accessKeyService.hash(provided)) -> {
                AccessKeyAuthentication(listOf(SimpleGrantedAuthority("ROLE_GRAPHQL")))
            }
            else -> FailedAccessKeyAuthentication(AccessKeyFailureReason.Invalid)
        }

        filterChain.doFilter(request, response)
    }
}
