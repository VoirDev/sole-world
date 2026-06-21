package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores cities and their coordinates within states or provinces. */
object CitiesTable : LongIdTable("cities") {
    /** Referenced state or province record. */
    val state = reference("state_id", StatesTable)

    /** Primary display name. */
    val name = varchar("name", 255)

    /** Latitude coordinate in decimal degrees. */
    val latitude = double("latitude")

    /** Longitude coordinate in decimal degrees. */
    val longitude = double("longitude")

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        CityTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (CityTranslationsTable.city eq id) and
                    (if (languageCode != null) (CityTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
