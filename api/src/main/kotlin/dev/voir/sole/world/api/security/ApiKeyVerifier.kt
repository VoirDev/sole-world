package dev.voir.sole.world.api.security

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * Verifies caller-supplied API keys against the keys configured for this container.
 *
 * Keys are never kept in memory as plain text. Each configured key is reduced to a SHA-256 digest at
 * startup, and a presented key is accepted only when its digest is among them. Comparing digests
 * rather than the secrets themselves is what makes the plain set lookup safe.
 */
@Component
@EnableConfigurationProperties(ApiKeyProperties::class)
class ApiKeyVerifier(properties: ApiKeyProperties) {
    /** Whether callers must present a known API key. */
    val enabled: Boolean = properties.enabled

    private val digests: Set<String>

    init {
        val keys = parseKeys(properties.keys)

        if (!enabled) {
            digests = emptySet()
            log.warn(
                "API key authentication is DISABLED (api.security.enabled=false). " +
                    "Every endpoint is reachable without credentials.",
            )
        } else {
            check(keys.isNotEmpty()) {
                "No API keys configured. Set the API_KEYS environment variable to one or more keys " +
                    "separated by commas, or set API_AUTH_ENABLED=false to serve this API publicly."
            }

            val tooShort = keys.count { it.length < MIN_KEY_LENGTH }
            check(tooShort == 0) {
                // Never echo the key itself; the count is enough to act on.
                "$tooShort configured API key(s) are shorter than $MIN_KEY_LENGTH characters. " +
                    "Use longer keys, for example the output of `openssl rand -hex 32`."
            }

            digests = keys.mapTo(mutableSetOf(), ::sha256Hex)
            log.info("API key authentication enabled with {} key(s)", digests.size)
        }
    }

    /**
     * Identifies the caller behind a presented key.
     *
     * The digest doubles as the caller's identity: it is stable per issued key and reveals nothing
     * about the secret, so it is safe to hold for per-caller accounting and to put in a log.
     *
     * @param presented Raw key taken from the request, or null when the caller supplied none.
     * @return Digest of an accepted key, [ANONYMOUS] when authentication is disabled, or null when
     *   the key is not one this deployment accepts.
     */
    fun identify(presented: String?): String? {
        if (!enabled) {
            return ANONYMOUS
        }

        val key = presented?.trim().orEmpty()
        if (key.isEmpty()) {
            return null
        }

        return sha256Hex(key).takeIf { it in digests }
    }

    companion object {
        /** Identity used for every caller when authentication is disabled. */
        const val ANONYMOUS = "anonymous"

        private val log = LoggerFactory.getLogger(ApiKeyVerifier::class.java)

        /** Shortest key accepted at startup; shorter secrets are not worth the false confidence. */
        private const val MIN_KEY_LENGTH = 16

        /** Splits the configured value on commas and any whitespace, dropping blanks and duplicates. */
        private fun parseKeys(raw: String): List<String> {
            return raw
                .split(',', ' ', '\t', '\n', '\r')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
        }

        private fun sha256Hex(value: String): String {
            // MessageDigest instances are not thread safe, so each call gets its own.
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString(separator = "") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        }
    }
}
