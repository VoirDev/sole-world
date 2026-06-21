package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.ClientsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API client record stored in the database.
 * @property id Primary key for the client.
 * @property name Human-readable client name.
 * @property description Optional operator-facing client description.
 * @property accessKeyPrefix Non-secret key prefix shown to operators.
 * @property active Whether the client key is currently accepted.
 */
data class ClientData(
    val id: Int,
    val name: String,
    val description: String?,
    val accessKeyPrefix: String,
    val active: Boolean,
) {
    companion object {
        /**
         * Converts a database row into a client data object.
         * @param row Exposed result row containing all client columns.
         * @return Client data mapped from the supplied row.
         */
        fun fromRow(row: ResultRow): ClientData {
            // Read through the table definition so column names stay centralized.
            return ClientData(
                id = row[ClientsTable.id].value,
                name = row[ClientsTable.name],
                description = row[ClientsTable.description],
                accessKeyPrefix = row[ClientsTable.accessKeyPrefix],
                active = row[ClientsTable.active],
            )
        }
    }
}
