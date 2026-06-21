package dev.voir.sole.world.api.graphql.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/** Successful client access-key authentication stored in the Spring security context. */
class AccessKeyAuthentication(
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(true)
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): Any = "graphql-api-key"
}

/** Failed client access-key authentication stored so resolver guards can explain rejection. */
class FailedAccessKeyAuthentication(
    val reason: AccessKeyFailureReason,
) : AbstractAuthenticationToken(emptyList()) {
    init {
        super.setAuthenticated(false)
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): Any = "graphql-api-key"
}

/** Reason a client access-key authentication attempt failed. */
enum class AccessKeyFailureReason {
    Missing,
    Invalid,
}
