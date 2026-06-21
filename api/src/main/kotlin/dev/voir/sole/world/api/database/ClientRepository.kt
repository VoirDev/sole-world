package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.ClientEntity
import dev.voir.sole.world.api.database.table.ClientsTable
import dev.voir.sole.world.api.model.ClientData
import dev.voir.sole.world.api.model.ClientData.Companion.fromRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

/** Repository for API client creation, updates, and access-key verification. */
@Repository
class ClientRepository {
    /** Returns all stored API clients ordered by id. */
    fun listClients(): List<ClientData> = transaction {
        ClientsTable
            .selectAll()
            .orderBy(ClientsTable.id)
            .map(::fromRow)
    }

    /**
     * Creates a new API client.
     * @param name Human-readable client name.
     * @param description Optional operator-facing client description.
     * @param accessKeyHash HMAC-SHA256 digest of the generated access key.
     * @param accessKeyPrefix Non-secret key prefix shown to operators.
     * @return Created client record.
     */
    fun createClient(
        name: String,
        description: String?,
        accessKeyHash: String,
        accessKeyPrefix: String,
    ): ClientData = transaction {
        val client = ClientEntity.new {
            this.name = name
            this.description = description
            this.accessKeyHash = accessKeyHash
            this.accessKeyPrefix = accessKeyPrefix
            this.active = true
        }

        ClientsTable
            .selectAll()
            .where { ClientsTable.id eq client.id.value }
            .map(::fromRow)
            .first()
    }

    /**
     * Updates an existing API client.
     * @param id Primary key of the client to update.
     * @param name Replacement human-readable client name.
     * @param description Replacement operator-facing client description.
     * @return Updated client record, or null when the client does not exist.
     */
    fun updateClient(
        id: Int,
        name: String,
        description: String?,
    ): ClientData? = transaction {
        val updatedRows = ClientsTable.update({ ClientsTable.id eq id }) {
            it[ClientsTable.name] = name
            it[ClientsTable.description] = description
        }

        if (updatedRows == 0) {
            return@transaction null
        }

        findClient(id)
    }

    /**
     * Replaces the API key for an existing client.
     * @param id Primary key of the client to update.
     * @param accessKeyHash HMAC-SHA256 digest of the generated access key.
     * @param accessKeyPrefix Non-secret key prefix shown to operators.
     * @return Updated client record, or null when the client does not exist.
     */
    fun rotateAccessKey(
        id: Int,
        accessKeyHash: String,
        accessKeyPrefix: String,
    ): ClientData? = transaction {
        val updatedRows = ClientsTable.update({ ClientsTable.id eq id }) {
            it[ClientsTable.accessKeyHash] = accessKeyHash
            it[ClientsTable.accessKeyPrefix] = accessKeyPrefix
            it[ClientsTable.active] = true
        }

        if (updatedRows == 0) {
            return@transaction null
        }

        findClient(id)
    }

    /**
     * Deletes an API client.
     * @param id Primary key of the client to delete.
     * @return True when a row was deleted.
     */
    fun deleteClient(id: Int): Boolean = transaction {
        ClientsTable.deleteWhere { ClientsTable.id eq id } > 0
    }

    private fun findClient(id: Int): ClientData? {
        return ClientsTable
            .selectAll()
            .where { ClientsTable.id eq id }
            .map(::fromRow)
            .firstOrNull()
    }

    /**
     * Checks whether an access key belongs to a stored API client.
     * @param accessKeyHash HMAC-SHA256 digest of the secret key presented by a caller.
     * @return True when the key exists in the clients table.
     */
    fun isValidAccessKeyHash(accessKeyHash: String): Boolean = transaction {
        ClientsTable
            .select(ClientsTable.id)
            .where { (ClientsTable.accessKeyHash eq accessKeyHash) and (ClientsTable.active eq true) }
            .limit(1)
            .any()
    }
}
