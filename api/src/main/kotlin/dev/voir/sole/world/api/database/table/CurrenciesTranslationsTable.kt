package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized currency names and descriptions. */
object CurrenciesTranslationsTable : LongIdTable("currency_translations") {
    /** Referenced currency record. */
    val currency = reference("currency_id", CurrenciesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)

    /** Human-readable description. */
    val description = text("description").nullable()
}
