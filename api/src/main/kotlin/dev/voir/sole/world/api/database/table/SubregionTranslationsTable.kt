package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized subregion names. */
object SubregionTranslationsTable : LongIdTable("subregion_translations") {
    /** Referenced parent subregion record. */
    val subregion = reference("subregion_id", SubregionsTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)
}
