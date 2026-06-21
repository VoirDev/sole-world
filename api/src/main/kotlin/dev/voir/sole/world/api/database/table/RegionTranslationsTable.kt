package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized region names. */
object RegionTranslationsTable : LongIdTable("region_translations") {
    /** Referenced parent region record. */
    val region = reference("region_id", RegionsTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)
}
