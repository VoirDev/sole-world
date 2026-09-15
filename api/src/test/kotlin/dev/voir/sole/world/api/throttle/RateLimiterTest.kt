package dev.voir.sole.world.api.throttle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RateLimiterTest {
    @Test
    fun `a caller may spend their whole burst back to back`() {
        val limiter = limiter(requestsPerMinute = 60, burst = 5)

        repeat(5) { assertInstanceOf(Allowed::class.java, limiter.tryAcquire("caller")) }
    }

    @Test
    fun `the next request past the burst is throttled`() {
        val limiter = limiter(requestsPerMinute = 60, burst = 5)
        repeat(5) { limiter.tryAcquire("caller") }

        val decision = limiter.tryAcquire("caller")

        val throttled = assertInstanceOf(Throttled::class.java, decision)
        // One token per second at 60/minute, and Retry-After never rounds down to zero.
        assertEquals(1L, throttled.retryAfterSeconds)
    }

    @Test
    fun `a slow sustained rate reports how long to wait`() {
        val limiter = limiter(requestsPerMinute = 6, burst = 1)
        limiter.tryAcquire("caller")

        val throttled = assertInstanceOf(Throttled::class.java, limiter.tryAcquire("caller"))

        // One token per ten seconds.
        assertTrue(throttled.retryAfterSeconds in 9..10, "waited ${throttled.retryAfterSeconds}s")
    }

    @Test
    fun `allowances are per caller`() {
        val limiter = limiter(requestsPerMinute = 60, burst = 2)
        repeat(2) { limiter.tryAcquire("noisy") }

        assertInstanceOf(Throttled::class.java, limiter.tryAcquire("noisy"))
        assertInstanceOf(Allowed::class.java, limiter.tryAcquire("quiet"))
    }

    @Test
    fun `nothing is throttled when the limit is switched off`() {
        val limiter = limiter(requestsPerMinute = 1, burst = 1, enabled = false)

        repeat(100) { assertInstanceOf(Allowed::class.java, limiter.tryAcquire("caller")) }
    }

    @Test
    fun `a nonsensical limit fails at startup rather than at request time`() {
        assertThrows<IllegalArgumentException> { limiter(requestsPerMinute = 0, burst = 5) }
        assertThrows<IllegalArgumentException> { limiter(requestsPerMinute = 60, burst = 0) }
    }

    private fun limiter(requestsPerMinute: Int, burst: Int, enabled: Boolean = true) =
        RateLimiter(
            RateLimitProperties(
                enabled = enabled,
                requestsPerMinute = requestsPerMinute,
                burst = burst,
            ),
        )
}
