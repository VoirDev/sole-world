package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for an API client and its access key.
 * @param id Exposed entity identifier for the row.
 */
class ClientEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ClientEntity>(ClientsTable)

    /** Human-readable client name used by operators. */
    var name by ClientsTable.name

    /** Optional operator-facing description for the client. */
    var description by ClientsTable.description

    /** HMAC-SHA256 digest of the secret access key presented by the client on API requests. */
    var accessKeyHash by ClientsTable.accessKeyHash

    /** Short non-secret key prefix shown to operators for identification. */
    var accessKeyPrefix by ClientsTable.accessKeyPrefix

    /** Whether this client key is currently accepted for API requests. */
    var active by ClientsTable.active
}
