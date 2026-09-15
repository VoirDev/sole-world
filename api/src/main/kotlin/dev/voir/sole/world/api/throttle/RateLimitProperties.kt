package dev.voir.sole.world.api.throttle

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Per-caller request limits applied to the data and GraphQL routes.
 *
 * A read API backed by an immutable in-memory dataset has no natural backpressure: every request is
 * served as fast as a core can serve it, so a single caller can occupy every worker thread. The
 * limit exists to keep one caller's traffic from becoming every caller's latency.
 *
 * @property enabled Whether requests are rate limited at all.
 * @property requestsPerMinute Sustained request rate allowed for one caller.
 * @property burst Requests a caller may make back to back before the sustained rate applies.
 */
@ConfigurationProperties(prefix = "api.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val requestsPerMinute: Int = 600,
    val burst: Int = 120,
)
