package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a language and its countries, flags, and translations.
 * @param id Exposed entity identifier for the row.
 */
class LanguageEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LanguageEntity>(LanguagesTable)

    /** ISO or application language code. */
    var code by LanguagesTable.code

    /** Name in the native language or script, when available. */
    var nativeName by LanguagesTable.nativeName

    /** Primary display name. */
    var name by LanguagesTable.name

    /** Human-readable description. */
    var description by LanguagesTable.description

    /** Raw shared flag foreign key value. */
    var flagId by LanguagesTable.flag

    /** Countries associated through a join table. */
    var countries by CountryEntity via CountryLanguagesTable

    /** Localized rows associated with this record. */
    val translations by LanguageTranslationEntity referrersOn LanguageTranslationsTable.language
}
