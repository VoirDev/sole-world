package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing localized currency text.
 * @param id Exposed entity identifier for the row.
 */
class CurrenciesTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CurrenciesTranslationEntity>(CurrenciesTranslationsTable)

    /** Referenced currency record. */
    var currency by CurrencyEntity referencedOn CurrenciesTranslationsTable.currency

    /** BCP 47-style language code for the localized text. */
    var languageCode by CurrenciesTranslationsTable.languageCode

    /** Primary display name. */
    var name by CurrenciesTranslationsTable.name

    /** Human-readable description. */
    var description by CurrenciesTranslationsTable.description
}
