package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.*
import dev.voir.sole.world.api.model.CountryData
import dev.voir.sole.world.api.model.CountryData.Companion.toCountry
import dev.voir.sole.world.api.model.NewCountryData
import dev.voir.sole.world.api.model.NewCountryTranslationData
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for country persistence, relationship management, search, and localized lookup operations. */
@Repository
class CountryRepository {
    /**
     * Checks whether a country row exists.
     * @param id Country primary key.
     * @return True when a country exists for the supplied id.
     */
    fun hasCountry(id: Int): Boolean = transaction {
        CountriesTable
            .select(CountriesTable.id)
            .where { CountriesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Batch inserts country rows used by the seed import.
     * @param countries Country seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createCountries(countries: List<NewCountryData>) = transaction {
        CountriesTable.batchInsert(countries) { country ->
            this[CountriesTable.id] = country.id

            this[CountriesTable.name] = country.name
            this[CountriesTable.nativeName] = country.nativeName

            this[CountriesTable.iso3] = country.iso3
            this[CountriesTable.iso2] = country.iso2
            this[CountriesTable.isoNumeric] = country.isoNumeric

            this[CountriesTable.phoneCode] = country.phoneCode
            this[CountriesTable.tld] = country.tld

            this[CountriesTable.longitude] = country.longitude
            this[CountriesTable.latitude] = country.latitude

            this[CountriesTable.flag] =
                country.flagId?.let { EntityID(it, FlagsTable) }

            this[CountriesTable.region] = country.regionId
            this[CountriesTable.subregion] = country.subregionId
        }
    }

    /**
     * Batch inserts localized country names used by the seed import.
     * @param translations Localized seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createCountryTranslations(translations: List<NewCountryTranslationData>) =
        transaction {
            CountryTranslationsTable.batchInsert(translations) { countryTranslationJSON ->
                this[CountryTranslationsTable.country] = countryTranslationJSON.countryId
                this[CountryTranslationsTable.languageCode] = countryTranslationJSON.languageCode
                this[CountryTranslationsTable.name] = countryTranslationJSON.name
            }
        }

    /**
     * Replaces the currencies associated with a country.
     * @param countryId Country primary key whose relationships should be replaced.
     * @param currencyIds Currency primary keys to associate.
     * @return Unit after the relationship collection is updated.
     */
    fun setCountryCurrencies(countryId: Int, currencyIds: List<Int>) = transaction {
        val country = CountryEntity.findById(countryId)
            ?: throw Exception("Country with ID $countryId not found")

        val currencies = currencyIds.map { id ->
            val currency =
                CurrencyEntity.find { CurrenciesTable.id eq id }.firstOrNull()
                    ?: throw Exception("Currency not found: $id")
            currency
        }
        country.currencies = SizedCollection(currencies)
    }

    /**
     * Replaces the timezones associated with a country.
     * @param countryId Country primary key whose relationships should be replaced.
     * @param timezoneIds Timezone primary keys to associate.
     * @return Unit after the relationship collection is updated.
     */
    fun setCountryTimezones(countryId: Int, timezoneIds: List<Long>) = transaction {
        val country = CountryEntity.findById(countryId)
            ?: throw Exception("Country with ID $countryId not found")

        val timezones = timezoneIds.map { id ->
            val timezone = TimezoneEntity.find { TimezonesTable.id eq id }.firstOrNull()
                ?: throw Exception("Timezone not found: $id")
            timezone
        }
        country.timezones = SizedCollection(timezones)
    }

    /**
     * Replaces the language relationships for a country, preserving the language type on each join row.
     * @param countryId Country primary key whose relationships should be replaced.
     * @param languageIds Language primary keys grouped by relationship type.
     * @return Unit after existing rows are replaced.
     */
    fun setCountryLanguages(
        countryId: Int,
        languageIds: Map<CountryLanguageType, List<Int>>,
    ) =
        transaction {
            CountryLanguagesTable.deleteWhere { CountryLanguagesTable.country eq countryId }

            val pairs = languageIds.flatMap { (type, ids) ->
                ids.map { id ->
                    Triple(
                        id,
                        type.name.lowercase(),
                        type
                    )
                }
            }

            CountryLanguagesTable.batchInsert(pairs) { (languageId, typeStr, _) ->
                this[CountryLanguagesTable.country] = EntityID(countryId, CountriesTable)
                this[CountryLanguagesTable.language] = EntityID(languageId, LanguagesTable)
                this[CountryLanguagesTable.type] = typeStr
            }
        }

    /**
     * Loads all countries, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All countries mapped to API data objects.
     */
    fun getAllCountries(languageCode: String?): List<CountryData> = transaction {
        if (languageCode == null) {
            return@transaction CountriesTable
                .selectAll()
                .orderBy(CountriesTable.name to SortOrder.ASC)
                .map { it.toCountry(null) }
        }

        return@transaction CountriesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(CountryTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[CountriesTable.id] }
            .map { it.toCountry(languageCode = languageCode) }
    }

    /**
     * Counts all country rows.
     * @return Total number of countries.
     */
    fun countCountries(): Long = transaction {
        CountriesTable.selectAll().count()
    }

    /**
     * Loads one page of countries, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Countries in the requested page.
     */
    fun getCountriesPage(languageCode: String?, offset: Long, limit: Int): List<CountryData> =
        transaction {
            if (languageCode == null) {
                return@transaction CountriesTable
                    .selectAll()
                    .orderBy(CountriesTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toCountry(null) }
            }

            return@transaction CountriesTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(CountryTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toCountry(languageCode = languageCode) }
        }

    /**
     * Searches countries across names, ISO codes, phone codes, domains, and flag emoji with relevance ordering.
     * @param query Free-text search value; compared case-insensitively.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param limit Maximum number of rows to return.
     * @return Countries matching the query, ordered by relevance.
     */
    fun searchCountries(query: String, languageCode: String?, limit: Int): List<CountryData> =
        transaction {
            val q = query.lowercase()
            val likeQuery = "%$q%"

            // TODO Support cyrillic search query
            CountriesTable
                .joinTranslations(languageCode)
                .select(CountriesTable.columns.plus(CountryTranslationsTable.columns))
                .where {
                    (CountriesTable.name.lowerCase() like likeQuery) or
                            (CountryTranslationsTable.name.lowerCase() like likeQuery) or
                            (CountriesTable.nativeName.lowerCase() like likeQuery) or
                            (CountriesTable.iso2.lowerCase() like likeQuery) or
                            (CountriesTable.iso3.lowerCase() like likeQuery) or
                            (CountriesTable.isoNumeric.lowerCase() like likeQuery) or
                            (CountriesTable.phoneCode.lowerCase() like likeQuery) or
                            (CountriesTable.tld.lowerCase() like likeQuery)
                }
                .orderBy(
                    Case()
                        .When(
                            CountriesTable.name.lowerCase() eq q,
                            intLiteral(100)
                        )
                        .When(
                            CountryTranslationsTable.name.lowerCase() eq q,
                            intLiteral(99)
                        )
                        .When(
                            CountriesTable.name.lowerCase() like "$q%",
                            intLiteral(95)
                        )
                        .When(
                            CountryTranslationsTable.name.lowerCase() like "$q%",
                            intLiteral(94)
                        )
                        .When(
                            CountriesTable.name.lowerCase() like likeQuery,
                            intLiteral(93)
                        )
                        .When(
                            CountryTranslationsTable.name.lowerCase() like likeQuery,
                            intLiteral(92)
                        )
                        .When(
                            CountriesTable.iso2.lowerCase() eq q,
                            intLiteral(75)
                        )
                        .When(
                            CountriesTable.iso2.lowerCase() like likeQuery,
                            intLiteral(60)
                        )
                        .When(
                            CountriesTable.iso3.lowerCase() eq q,
                            intLiteral(70)
                        )
                        .When(
                            CountriesTable.iso3.lowerCase() like likeQuery,
                            intLiteral(55)
                        )
                        .When(
                            CountriesTable.nativeName.lowerCase() like likeQuery,
                            intLiteral(50)
                        )
                        .When(
                            CountriesTable.phoneCode.lowerCase() eq q,
                            intLiteral(40)
                        )
                        .When(
                            CountriesTable.tld.lowerCase() like likeQuery,
                            intLiteral(30)
                        )
                        .Else(intLiteral(0)) to SortOrder.DESC
                )
                .limit(limit)
                .distinctBy { it[CountriesTable.id] }
                .map { it.toCountry(languageCode = languageCode) }
        }

    /**
     * Loads a single country by primary key.
     * @param id Country primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching country, or null when it does not exist.
     */
    fun getCountryById(id: Int, languageCode: String?): CountryData? = transaction {
        val query = CountriesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CountriesTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toCountry(languageCode = languageCode)
    }

    /**
     * Loads countries by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Countries matching the supplied ids.
     */
    fun getCountriesByIds(
        ids: List<Int>,
        languageCode: String?
    ): List<CountryData> = transaction {
        CountriesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CountriesTable.id inList ids }
            .distinctBy { it[CountriesTable.id] }
            .map { it.toCountry(languageCode = languageCode) }
    }

    /**
     * Loads countries belonging to any of the supplied subregions.
     * @param ids Subregion ids used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Countries in the supplied subregions.
     */
    fun getCountriesBySubregionIds(
        ids: Set<Int>,
        languageCode: String?
    ): List<CountryData> = transaction {
        CountriesTable
            .joinTranslations(languageCode)
            .innerJoin(SubregionsTable)
            .selectAll()
            .where { SubregionsTable.id inList ids }
            .distinctBy { it[CountriesTable.id] }
            .map { it.toCountry(languageCode = languageCode) }
    }

    /**
     * Loads countries grouped by subregion id for batched GraphQL resolution.
     * @param ids Subregion ids used to group related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by subregion id with matching countries.
     */
    fun getCountriesGroupedBySubregionIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CountryData>> = transaction {
        val countries = if (languageCode == null) {
            CountriesTable
        } else {
            CountriesTable.joinTranslations(languageCode)
        }

        countries
            .selectAll()
            .where { CountriesTable.subregion inList ids }
            .groupBy(
                keySelector = { it[CountriesTable.subregion].value },
                valueTransform = { row -> row.toCountry(languageCode = languageCode) }
            )
            .mapValues { (_, countriesForSubregion) ->
                countriesForSubregion.distinctBy { it.id }
            }
    }

    /**
     * Loads countries belonging to a single region.
     * @param regionId Region primary key used to filter countries.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Countries in the supplied region.
     */
    fun getCountriesByRegionId(
        regionId: Int,
        languageCode: String?
    ): List<CountryData> =
        transaction {
            CountriesTable
                .joinTranslations(languageCode)
                .innerJoin(RegionsTable)
                .select(CountriesTable.columns)
                .where { RegionsTable.id eq regionId }
                .distinctBy { it[CountriesTable.id] }
                .map { it.toCountry(languageCode = languageCode) }
        }


    /**
     * Loads countries grouped by currency id for batched GraphQL resolution.
     * @param ids Currency ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by currency id with matching countries.
     */
    fun getCountriesByCurrencyIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CountryData>> = transaction {
        val countries = if (languageCode == null) {
            CountriesTable
        } else {
            CountriesTable.joinTranslations(languageCode)
        }

        countries
            .innerJoin(CountryCurrenciesTable)
            .selectAll()
            .where { CountryCurrenciesTable.currency inList ids }
            .groupBy(
                keySelector = { it[CountryCurrenciesTable.currency].value },
                valueTransform = { row -> row.toCountry(languageCode) }
            )
            .mapValues { (_, countriesForCurrency) ->
                countriesForCurrency.distinctBy { it.id }
            }
    }

    /**
     * Loads countries grouped by language id for batched GraphQL resolution.
     * @param ids Language ids used to group related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by language id with matching countries.
     */
    fun getCountriesByLanguageIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CountryData>> = transaction {
        val countries = if (languageCode == null) {
            CountriesTable
        } else {
            CountriesTable.joinTranslations(languageCode)
        }

        countries
            .innerJoin(CountryLanguagesTable)
            .selectAll()
            .where { CountryLanguagesTable.language inList ids }
            .groupBy(
                keySelector = { it[CountryLanguagesTable.language].value },
                valueTransform = { row -> row.toCountry(languageCode) }
            )
            .mapValues { (_, countriesForLanguage) ->
                countriesForLanguage.distinctBy { it.id }
            }
    }

    /**
     * Loads countries belonging to any of the supplied regions.
     * @param ids Region ids used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Countries in the supplied regions.
     */
    fun getCountriesByRegionIds(
        ids: Set<Int>,
        languageCode: String?
    ): List<CountryData> = transaction {
        CountriesTable
            .joinTranslations(languageCode)
            .innerJoin(RegionsTable)
            .selectAll()
            .where { RegionsTable.id inList ids }
            .distinctBy { it[CountriesTable.id] }
            .map { it.toCountry(languageCode = languageCode) }
    }

    /**
     * Loads countries grouped by region id for batched GraphQL resolution.
     * @param ids Region ids used to group related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by region id with matching countries.
     */
    fun getCountriesGroupedByRegionIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CountryData>> = transaction {
        val countries = if (languageCode == null) {
            CountriesTable
        } else {
            CountriesTable.joinTranslations(languageCode)
        }

        countries
            .selectAll()
            .where { CountriesTable.region inList ids }
            .groupBy(
                keySelector = { it[CountriesTable.region].value },
                valueTransform = { row -> row.toCountry(languageCode = languageCode) }
            )
            .mapValues { (_, countriesForRegion) ->
                countriesForRegion.distinctBy { it.id }
            }
    }
}
