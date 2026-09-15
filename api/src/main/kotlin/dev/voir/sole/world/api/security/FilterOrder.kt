package dev.voir.sole.world.api.security

/**
 * Positions of this application's servlet filters relative to one another.
 *
 * The order is part of the security model rather than an implementation detail. Response headers
 * are set first so that a rejected request carries them too; authentication runs before rate
 * limiting so that an unauthenticated flood cannot spend anyone's allowance, and so that the rate
 * limiter has a verified identity to account against.
 */
object FilterOrder {
    /** Sets the response headers that hold for every route, including a rejected one. */
    const val RESPONSE_HEADERS = 50

    /** Rejects requests without a configured API key. */
    const val AUTHENTICATION = 100

    /** Spends one request from the authenticated caller's allowance. */
    const val RATE_LIMIT = 200
}
