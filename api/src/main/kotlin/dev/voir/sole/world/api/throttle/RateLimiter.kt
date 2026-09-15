package dev.voir.sole.world.api.throttle

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decides whether a caller may make one more request.
 *
 * Each caller gets a token bucket: it holds [RateLimitProperties.burst] tokens, refills at
 * [RateLimitProperties.requestsPerMinute], and a request spends one. A bucket lets a caller burst
 * through a page-by-page walk of a resource and then settles them at the sustained rate, which fits
 * how this API is actually used better than a fixed window would.
 *
 * State is per replica and held in memory. That is deliberate: replicas are independent by design,
 * and a shared store would reintroduce the external dependency this service exists to avoid. A
 * deployment behind several replicas enforces the limit per replica, which the README documents.
 */
@Component
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimiter(private val properties: RateLimitProperties) {
    /** Whether this deployment limits request rates. */
    val enabled: Boolean = properties.enabled

    private val capacity: Double = properties.burst.toDouble()

    private val tokensPerNano: Double = properties.requestsPerMinute / (SECONDS_PER_MINUTE * NANOS_PER_SECOND)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    private val sweeping = AtomicBoolean(false)

    init {
        require(properties.requestsPerMinute > 0) { "api.rate-limit.requests-per-minute must be positive" }
        require(properties.burst > 0) { "api.rate-limit.burst must be positive" }
    }

    /**
     * Spends one request from a caller's allowance.
     * @param caller Stable caller identifier.
     * @return [Allowed] when the request may proceed, otherwise how long the caller should wait.
     */
    fun tryAcquire(caller: String): RateLimitDecision {
        if (!enabled) {
            return Allowed
        }

        if (buckets.size >= MAX_TRACKED_CALLERS) {
            sweepIdleBuckets()
        }

        return buckets.computeIfAbsent(caller) { Bucket(capacity) }.spend()
    }

    /**
     * Drops buckets that have sat idle long enough to have refilled completely.
     *
     * A full bucket is indistinguishable from a caller that has never been seen, so forgetting one
     * costs a caller nothing and keeps the map from growing with every distinct client address.
     */
    private fun sweepIdleBuckets() {
        if (!sweeping.compareAndSet(false, true)) {
            return
        }

        try {
            val now = System.nanoTime()
            val idleThreshold = (capacity / tokensPerNano).toLong()
            buckets.values.removeIf { now - it.lastSpendNanos > idleThreshold }
        } finally {
            sweeping.set(false)
        }
    }

    private inner class Bucket(private var tokens: Double) {
        @Volatile
        var lastSpendNanos: Long = System.nanoTime()
            private set

        @Synchronized
        fun spend(): RateLimitDecision {
            val now = System.nanoTime()
            tokens = minOf(capacity, tokens + (now - lastSpendNanos) * tokensPerNano)
            lastSpendNanos = now

            if (tokens >= 1.0) {
                tokens -= 1.0
                return Allowed
            }

            val nanosToNextToken = ((1.0 - tokens) / tokensPerNano).toLong()
            // Round up: a Retry-After of zero invites an immediate retry that would fail again.
            val secondsToWait = (nanosToNextToken + NANOS_PER_SECOND.toLong() - 1) / NANOS_PER_SECOND.toLong()

            return Throttled(retryAfterSeconds = maxOf(1, secondsToWait))
        }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
        const val NANOS_PER_SECOND = 1_000_000_000.0

        /** Buckets held before idle ones are swept; well above any plausible number of live callers. */
        const val MAX_TRACKED_CALLERS = 10_000
    }
}

/** Outcome of asking to spend one request from a caller's allowance. */
sealed interface RateLimitDecision

/** The caller had allowance left. */
data object Allowed : RateLimitDecision

/**
 * The caller has spent their allowance.
 * @property retryAfterSeconds Whole seconds until the caller's next request would be allowed.
 */
data class Throttled(val retryAfterSeconds: Long) : RateLimitDecision
