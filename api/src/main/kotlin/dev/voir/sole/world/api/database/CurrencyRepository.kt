package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.CountryCurrenciesTable
import dev.voir.sole.world.api.database.table.CurrenciesTable
import dev.voir.sole.world.api.database.table.CurrenciesTranslationsTable
import dev.voir.sole.world.api.model.CurrencyData
import dev.voir.sole.world.api.model.CurrencyData.Companion.toCurrency
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for currency lookup operations and country-currency grouping queries. */
@Repository
class CurrencyRepository {
    /**
     * Loads all currencies, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All currencies mapped to API data objects.
     */
    fun getAllCurrencies(languageCode: String?): List<CurrencyData> = transaction {
        if (languageCode == null) {
            return@transaction CurrenciesTable
                .selectAll()
                .orderBy(CurrenciesTable.name to SortOrder.ASC)
                .map { it.toCurrency(null) }
        }

        return@transaction CurrenciesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(CurrenciesTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[CurrenciesTable.id] }
            .map { it.toCurrency(languageCode = languageCode) }
    }

    /**
     * Counts all currency rows.
     * @return Total number of currencies.
     */
    fun countCurrencies(): Long = transaction {
        CurrenciesTable.selectAll().count()
    }

    /**
     * Loads one page of currencies, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Currencies in the requested page.
     */
    fun getCurrenciesPage(languageCode: String?, offset: Long, limit: Int): List<CurrencyData> =
        transaction {
            if (languageCode == null) {
                return@transaction CurrenciesTable
                    .selectAll()
                    .orderBy(CurrenciesTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toCurrency(null) }
            }

            return@transaction CurrenciesTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(CurrenciesTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toCurrency(languageCode = languageCode) }
        }

    /**
     * Resolves a currency by ISO alpha or numeric code.
     * @param identifier ISO alpha or numeric currency identifier.
     * @param withObsolete When true, obsolete currencies may be returned.
     * @return The matching currency, or null when no active match is found.
     */
    fun resolveCurrency(identifier: String, withObsolete: Boolean?): CurrencyData? = transaction {
        val idLowerCase = identifier.lowercase()

        var condition: Op<Boolean> = (LowerCase(CurrenciesTable.iso3) eq idLowerCase) or
                (LowerCase(CurrenciesTable.isoNumeric) eq idLowerCase)

        if (withObsolete != true) {
            condition = condition and (CurrenciesTable.obsolete eq false)
        }

        CurrenciesTable
            .selectAll()
            .where { condition }
            .limit(1)
            .firstOrNull()
            ?.toCurrency(languageCode = null)
    }

    /**
     * Checks whether a currency row exists.
     * @param id Currency primary key.
     * @return True when a currency exists for the supplied id.
     */
    fun hasCurrency(id: Int): Boolean = transaction {
        CurrenciesTable
            .select(CurrenciesTable.id)
            .where { CurrenciesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Loads a single currency by primary key.
     * @param id Currency primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching currency, or null when it does not exist.
     */
    suspend fun getCurrencyById(id: Int, languageCode: String?): CurrencyData? = transaction {
        val query = CurrenciesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CurrenciesTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toCurrency(languageCode = languageCode)
    }

    /**
     * Loads currencies by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Currencies matching the supplied ids.
     */
    suspend fun getCurrenciesByIds(ids: List<Int>, languageCode: String?): List<CurrencyData> =
        transaction {
            CurrenciesTable
                .joinTranslations(languageCode)
                .selectAll()
                .where { CurrenciesTable.id inList ids }
                .distinctBy { it[CurrenciesTable.id] }
                .map { it.toCurrency(languageCode = languageCode) }
        }

    /**
     * Loads currencies grouped by country id for batched GraphQL resolution.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching currencies.
     */
    fun getCurrenciesByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CurrencyData>> = transaction {
        val currencies = if (languageCode == null) {
            CurrenciesTable
        } else {
            CurrenciesTable.joinTranslations(languageCode)
        }

        currencies
            .innerJoin(CountryCurrenciesTable)
            .selectAll()
            .where { CountryCurrenciesTable.country inList ids }
            .groupBy(
                keySelector = { it[CountryCurrenciesTable.country].value },
                valueTransform = { row -> row.toCurrency(languageCode) }
            )
            .mapValues { (_, currenciesForCountry) ->
                currenciesForCountry.distinctBy { it.id }
            }
    }
}
