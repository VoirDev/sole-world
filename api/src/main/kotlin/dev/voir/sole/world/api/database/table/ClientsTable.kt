package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/** Stores API clients that can access protected GraphQL endpoints. */
object ClientsTable : IntIdTable("clients") {
    /** Human-readable client name used by operators. */
    val name = varchar("name", 255)

    /** Optional operator-facing description for the client. */
    val description = text("description").nullable()

    /** HMAC-SHA256 digest of the secret access key presented by the client on API requests. */
    val accessKeyHash = varchar("access_key_hash", 64).uniqueIndex()

    /** Short non-secret key prefix shown to operators for identification. */
    val accessKeyPrefix = varchar("access_key_prefix", 32)

    /** Whether this client key is currently accepted for API requests. */
    val active = bool("active")
}
