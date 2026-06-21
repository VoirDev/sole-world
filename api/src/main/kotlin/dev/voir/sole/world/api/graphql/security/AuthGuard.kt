package dev.voir.sole.world.api.graphql.security

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Method-security guard used by the [Authenticated] annotation. */
@Component("authGuard")
class AuthGuard {
    /** Allows annotated resolver execution only when a valid client access key was provided. */
    fun requireAuthenticated(): Boolean {
        val auth = SecurityContextHolder.getContext().authentication

        if (auth is FailedAccessKeyAuthentication) {
            throw AccessDeniedException(auth.reason.message)
        }

        if (auth !is AccessKeyAuthentication || !auth.isAuthenticated) {
            throw AccessDeniedException(AccessKeyFailureReason.Missing.message)
        }

        return true
    }

    private val AccessKeyFailureReason.message: String
        get() = when (this) {
            AccessKeyFailureReason.Missing -> "Missing API access key"
            AccessKeyFailureReason.Invalid -> "Invalid API access key"
        }
}
