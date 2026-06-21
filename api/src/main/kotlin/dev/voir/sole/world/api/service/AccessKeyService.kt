package dev.voir.sole.world.api.service

import dev.voir.sole.world.api.configs.ClientSecurityProperties
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Generates and hashes client API access keys. */
@Service
class AccessKeyService(
    private val clientSecurityProperties: ClientSecurityProperties,
) {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /** Creates a new random access key and its persisted lookup fields. */
    fun generate(): GeneratedAccessKey {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)

        val raw = "sole_live_${encoder.encodeToString(bytes)}"
        return GeneratedAccessKey(
            raw = raw,
            hash = hash(raw),
            prefix = raw.take(18),
        )
    }

    /** Calculates a server-secret HMAC digest for constant database lookup. */
    fun hash(raw: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(
            clientSecurityProperties.accessKeyHashSecret.toByteArray(Charsets.UTF_8),
            "HmacSHA256",
        )
        mac.init(key)

        return mac.doFinal(raw.toByteArray(Charsets.UTF_8)).joinToString(separator = "") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }
}

/** Access key material split into returned and stored pieces. */
data class GeneratedAccessKey(
    val raw: String,
    val hash: String,
    val prefix: String,
)
