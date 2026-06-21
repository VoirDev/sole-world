package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.CountryLanguagesTable
import dev.voir.sole.world.api.database.table.LanguageTranslationsTable
import dev.voir.sole.world.api.database.table.LanguagesTable
import dev.voir.sole.world.api.model.LanguageData
import dev.voir.sole.world.api.model.LanguageData.Companion.toLanguage
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for language lookup operations and country-language grouping queries. */
@Repository
class LanguageRepository {
    /**
     * Checks whether a language row exists.
     * @param id Language primary key.
     * @return True when a language exists for the supplied id.
     */
    fun hasLanguage(id: Int): Boolean = transaction {
        LanguagesTable
            .select(LanguagesTable.id)
            .where { LanguagesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Loads all languages, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All languages mapped to API data objects.
     */
    fun getAll(languageCode: String?): List<LanguageData> = transaction {
        if (languageCode == null) {
            return@transaction LanguagesTable
                .selectAll()
                .orderBy(LanguagesTable.name to SortOrder.ASC)
                .map { it.toLanguage(null) }
        }

        return@transaction LanguagesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(LanguageTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[LanguagesTable.id] }
            .map { it.toLanguage(languageCode = languageCode) }
    }

    /**
     * Counts all language rows.
     * @return Total number of languages.
     */
    fun countLanguages(): Long = transaction {
        LanguagesTable.selectAll().count()
    }

    /**
     * Loads one page of languages, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Languages in the requested page.
     */
    fun getLanguagesPage(languageCode: String?, offset: Long, limit: Int): List<LanguageData> =
        transaction {
            if (languageCode == null) {
                return@transaction LanguagesTable
                    .selectAll()
                    .orderBy(LanguagesTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toLanguage(null) }
            }

            return@transaction LanguagesTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(LanguageTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toLanguage(languageCode = languageCode) }
        }

    /**
     * Loads languages by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Languages matching the supplied ids.
     */
    fun getLanguagesByIds(
        ids: List<Int>,
        languageCode: String?
    ): List<LanguageData> = transaction {
        val languages = if (languageCode == null) {
            LanguagesTable
        } else {
            LanguagesTable.joinTranslations(languageCode)
        }

        languages
            .selectAll()
            .where { LanguagesTable.id inList ids }
            .distinctBy { it[LanguagesTable.id] }
            .map { it.toLanguage(languageCode = languageCode) }
    }

    /**
     * Loads languages grouped by country id for batched GraphQL resolution.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching languages.
     */
    fun getLanguagesByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<LanguageData>> = transaction {
        val languages = if (languageCode == null) {
            LanguagesTable
        } else {
            LanguagesTable.joinTranslations(languageCode)
        }

        languages
            .innerJoin(CountryLanguagesTable)
            .selectAll()
            .where { CountryLanguagesTable.country inList ids }
            .groupBy(
                keySelector = { it[CountryLanguagesTable.country].value },
                valueTransform = { row -> row.toLanguage(languageCode) }
            )
            .mapValues { (_, languagesForCountry) ->
                languagesForCountry.distinctBy { it.id }
            }
    }
}
