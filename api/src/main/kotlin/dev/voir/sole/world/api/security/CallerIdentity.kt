package dev.voir.sole.world.api.security

import jakarta.servlet.http.HttpServletRequest

/**
 * Identifies the caller behind a request for per-caller accounting.
 *
 * When authentication is on, the identity is the digest of the presented key, which
 * [ApiKeyFilter] records once it has verified it — one allowance per issued key, and never the key
 * itself in memory or in a log. When authentication is off there is no key to attribute a request
 * to, so the remote address stands in.
 */
object CallerIdentity {
    /** Request attribute carrying the verified caller identity. */
    const val ATTRIBUTE = "dev.voir.sole.world.api.callerIdentity"

    /**
     * Resolves the identity to account a request against.
     * @param request Incoming HTTP request.
     * @return Verified key digest when one is present, otherwise the caller's address.
     */
    fun of(request: HttpServletRequest): String =
        request.getAttribute(ATTRIBUTE) as? String
            ?: request.remoteAddr
            ?: UNKNOWN

    private const val UNKNOWN = "unknown"
}
