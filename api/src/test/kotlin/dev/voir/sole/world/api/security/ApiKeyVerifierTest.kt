package dev.voir.sole.world.api.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ApiKeyVerifierTest {
    @Test
    fun `accepts a configured key`() {
        val verifier = verifier("first-key-0123456789")

        assertNotNull(verifier.identify("first-key-0123456789"))
    }

    @Test
    fun `accepts any key from a comma separated list`() {
        val verifier = verifier("first-key-0123456789, second-key-0123456789")

        assertNotNull(verifier.identify("first-key-0123456789"))
        assertNotNull(verifier.identify("second-key-0123456789"))
    }

    @Test
    fun `accepts keys separated by whitespace and newlines`() {
        val verifier = verifier("first-key-0123456789\n  second-key-0123456789\t")

        assertNotNull(verifier.identify("first-key-0123456789"))
        assertNotNull(verifier.identify("second-key-0123456789"))
    }

    @Test
    fun `trims surrounding whitespace from the presented key`() {
        val verifier = verifier("first-key-0123456789")

        assertNotNull(verifier.identify("  first-key-0123456789  "))
    }

    @Test
    fun `rejects an unknown key`() {
        val verifier = verifier("first-key-0123456789")

        assertNull(verifier.identify("second-key-0123456789"))
    }

    @Test
    fun `rejects a missing or blank key`() {
        val verifier = verifier("first-key-0123456789")

        assertNull(verifier.identify(null))
        assertNull(verifier.identify(""))
        assertNull(verifier.identify("   "))
    }

    @Test
    fun `key comparison is case sensitive`() {
        val verifier = verifier("First-Key-0123456789")

        assertNull(verifier.identify("first-key-0123456789"))
    }

    @Test
    fun `each key gets its own stable identity, and none of them is the key`() {
        val verifier = verifier("first-key-0123456789, second-key-0123456789")

        val first = verifier.identify("first-key-0123456789")
        val second = verifier.identify("second-key-0123456789")

        assertEquals(first, verifier.identify("first-key-0123456789"))
        assertNotEquals(first, second)
        assertFalse(first!!.contains("first-key"))
    }

    @Test
    fun `fails to start when no keys are configured`() {
        val failure = assertThrows<IllegalStateException> { verifier("") }

        assertTrue(failure.message!!.contains("API_KEYS"))
    }

    @Test
    fun `fails to start when a configured key is too short`() {
        val failure = assertThrows<IllegalStateException> { verifier("short") }

        assertTrue(failure.message!!.contains("16 characters"))
        // The key itself must never appear in the failure message.
        assertFalse(failure.message!!.contains("short "))
    }

    @Test
    fun `accepts anything when authentication is disabled`() {
        val verifier = ApiKeyVerifier(ApiKeyProperties(enabled = false, keys = ""))

        assertEquals(ApiKeyVerifier.ANONYMOUS, verifier.identify(null))
        assertEquals(ApiKeyVerifier.ANONYMOUS, verifier.identify("anything"))
    }

    private fun verifier(keys: String) = ApiKeyVerifier(ApiKeyProperties(enabled = true, keys = keys))
}
