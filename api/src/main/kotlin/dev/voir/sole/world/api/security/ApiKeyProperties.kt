package dev.voir.sole.world.api.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * API access-key settings supplied by the container environment.
 * @property enabled Whether callers must present a known API key.
 * @property keys Raw key list separated by commas or whitespace.
 */
@ConfigurationProperties(prefix = "api.security")
data class ApiKeyProperties(
    val enabled: Boolean = true,
    val keys: String = "",
)
