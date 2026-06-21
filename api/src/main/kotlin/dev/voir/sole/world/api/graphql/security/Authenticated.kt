package dev.voir.sole.world.api.graphql.security

import org.springframework.security.access.prepost.PreAuthorize

/** Marks a GraphQL query or mutation resolver as requiring a valid client access key. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authGuard.requireAuthenticated()")
annotation class Authenticated
