package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.CitiesTable
import dev.voir.sole.world.api.database.table.CityTranslationsTable
import dev.voir.sole.world.api.database.table.StatesTable
import dev.voir.sole.world.api.model.CityData
import dev.voir.sole.world.api.model.CityData.Companion.toCity
import dev.voir.sole.world.api.model.NewCityData
import dev.voir.sole.world.api.model.NewCityTranslation
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for city persistence and localized lookup operations. */
@Repository
class CityRepository {
    /**
     * Checks whether a city row exists.
     * @param id City primary key.
     * @return True when a city exists for the supplied id.
     */
    fun hasCity(id: Long): Boolean = transaction {
        CitiesTable
            .select(CitiesTable.id)
            .where { CitiesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Batch inserts city rows used by the seed import.
     * @param cities City seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createCities(cities: List<NewCityData>) = transaction {
        CitiesTable.batchInsert(cities) { city ->
            this[CitiesTable.id] = city.id
            this[CitiesTable.name] = city.name
            this[CitiesTable.state] = city.stateId
            this[CitiesTable.longitude] = city.longitude
            this[CitiesTable.latitude] = city.latitude
        }
    }

    /**
     * Batch inserts localized city names used by the seed import.
     * @param translations Localized seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createCityTranslations(translations: List<NewCityTranslation>) = transaction {
        CityTranslationsTable.batchInsert(translations) { translation ->
            this[CityTranslationsTable.city] = translation.cityId
            this[CityTranslationsTable.languageCode] = translation.languageCode
            this[CityTranslationsTable.name] = translation.name
        }
    }

    /**
     * Loads all cities, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All cities mapped to API data objects.
     */
    fun getAllCities(languageCode: String?): List<CityData> = transaction {
        if (languageCode == null) {
            return@transaction CitiesTable
                .selectAll()
                .orderBy(CitiesTable.name to SortOrder.ASC)
                .map { it.toCity(null) }
        }

        return@transaction CitiesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(CityTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[CitiesTable.id] }
            .map { it.toCity(languageCode = languageCode) }
    }

    /**
     * Counts all city rows.
     * @return Total number of cities.
     */
    fun countCities(): Long = transaction {
        CitiesTable.selectAll().count()
    }

    /**
     * Counts city rows grouped under one country through its states.
     * @param countryId Country primary key used to filter related rows.
     * @return Total number of cities in the country.
     */
    fun countCitiesByCountryId(
        countryId: Int,
        languageCode: String?,
        query: String?,
    ): Long = transaction {
        if (query == null) {
            return@transaction CitiesTable
                .innerJoin(StatesTable)
                .select(CitiesTable.id)
                .where { StatesTable.country eq countryId }
                .count()
        }

        val likeQuery = "%${query.lowercase()}%"

        if (languageCode == null) {
            return@transaction CitiesTable
                .innerJoin(StatesTable)
                .select(CitiesTable.id)
                .where {
                    (StatesTable.country eq countryId) and
                            (CitiesTable.name.lowerCase() like likeQuery)
                }
                .count()
        }

        CitiesTable
            .joinTranslations(languageCode)
            .innerJoin(StatesTable)
            .select(CitiesTable.id)
            .where {
                (StatesTable.country eq countryId) and
                        (
                                (CitiesTable.name.lowerCase() like likeQuery) or
                                        (CityTranslationsTable.name.lowerCase() like likeQuery)
                                )
            }
            .count()
    }

    /**
     * Loads one page of cities, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Cities in the requested page.
     */
    fun getCitiesPage(languageCode: String?, offset: Long, limit: Int): List<CityData> =
        transaction {
            if (languageCode == null) {
                return@transaction CitiesTable
                    .selectAll()
                    .orderBy(CitiesTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toCity(null) }
            }

            return@transaction CitiesTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(CityTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toCity(languageCode = languageCode) }
        }

    /**
     * Loads a single city by primary key.
     * @param id City primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching city, or null when it does not exist.
     */
    fun getCityById(id: Long, languageCode: String?): CityData? = transaction {
        val query = CitiesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CitiesTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toCity(languageCode = languageCode)
    }

    /**
     * Loads cities by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Cities matching the supplied ids.
     */
    fun getCitiesByIds(
        ids: List<Long>,
        languageCode: String?
    ): List<CityData> = transaction {
        CitiesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CitiesTable.id inList ids }
            .distinctBy { it[CitiesTable.id] }
            .map { it.toCity(languageCode = languageCode) }
    }

    /**
     * Loads cities grouped by country id through their states.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching cities.
     */
    fun getCitiesByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CityData>> = transaction {
        val cities = if (languageCode == null) {
            CitiesTable
        } else {
            CitiesTable.joinTranslations(languageCode)
        }

        cities
            .innerJoin(StatesTable)
            .selectAll()
            .where { StatesTable.country inList ids }
            .groupBy(
                keySelector = { it[StatesTable.country].value },
                valueTransform = { row -> row.toCity(languageCode) }
            )
            .mapValues { (_, citiesForCountry) ->
                citiesForCountry.distinctBy { it.id }
            }
    }

    /**
     * Loads one page of cities grouped under a country through its states.
     * @param countryId Country primary key used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Requested country city page mapped to API data objects.
     */
    fun getCitiesPageByCountryId(
        countryId: Int,
        languageCode: String?,
        query: String?,
        offset: Long,
        limit: Int,
    ): List<CityData> = transaction {
        val likeQuery = query?.lowercase()?.let { "%$it%" }
        val cities = if (languageCode == null) {
            CitiesTable
        } else {
            CitiesTable.joinTranslations(languageCode)
        }

        val orderColumn = if (languageCode == null) {
            CitiesTable.name
        } else {
            CityTranslationsTable.name
        }

        cities
            .innerJoin(StatesTable)
            .selectAll()
            .where {
                if (likeQuery == null) {
                    StatesTable.country eq countryId
                } else if (languageCode == null) {
                    (StatesTable.country eq countryId) and
                            (CitiesTable.name.lowerCase() like likeQuery)
                } else {
                    (StatesTable.country eq countryId) and
                            (
                                    (CitiesTable.name.lowerCase() like likeQuery) or
                                            (CityTranslationsTable.name.lowerCase() like likeQuery)
                                    )
                }
            }
            .orderBy(orderColumn to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .map { it.toCity(languageCode) }
    }
}
