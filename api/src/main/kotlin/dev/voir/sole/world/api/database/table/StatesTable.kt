package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores first-level administrative divisions for countries. */
object StatesTable : LongIdTable("states") {
    /** Primary display name. */
    val name = varchar("name", 255)

    /** State or province code, when available. */
    val stateCode = varchar("state_code", 10).nullable()

    /** Latitude coordinate in decimal degrees. */
    val latitude = double("latitude").nullable()

    /** Longitude coordinate in decimal degrees. */
    val longitude = double("longitude").nullable()

    /** Classification value for this record. */
    val type = varchar("type", 64).nullable()

    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        StateTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (StateTranslationsTable.state eq id) and
                    (if (languageCode != null) (StateTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
