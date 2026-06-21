package dev.voir.sole.world.api.configs

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedMethods: List<String>,
    val allowedOrigins: List<String>,
    val allowedHeaders: List<String>,
)
