package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.CountriesTable
import dev.voir.sole.world.api.database.table.StateTranslationsTable
import dev.voir.sole.world.api.database.table.StatesTable
import dev.voir.sole.world.api.model.NewStateData
import dev.voir.sole.world.api.model.NewStateTranslationData
import dev.voir.sole.world.api.model.StateData
import dev.voir.sole.world.api.model.StateData.Companion.toState
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for state or province persistence and localized lookup operations. */
@Repository
class StateRepository {
    /**
     * Checks whether a state or province row exists.
     * @param id State or province primary key.
     * @return True when a state or province exists for the supplied id.
     */
    fun hasState(id: Long): Boolean = transaction {
        StatesTable
            .select(StatesTable.id)
            .where { StatesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Batch inserts state or province rows used by the seed import.
     * @param states State or province seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createStates(states: List<NewStateData>) = transaction {
        StatesTable.batchInsert(states) { state ->
            this[StatesTable.id] = state.id
            this[StatesTable.name] = state.name
            this[StatesTable.stateCode] = state.stateCode
            this[StatesTable.type] = state.type
            this[StatesTable.latitude] = state.latitude
            this[StatesTable.longitude] = state.longitude
            this[StatesTable.country] = state.countryId
        }
    }

    /**
     * Batch inserts localized state or province names used by the seed import.
     * @param translations Localized seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createStateTranslations(translations: List<NewStateTranslationData>) = transaction {
        StateTranslationsTable.batchInsert(translations) { translation ->
            this[StateTranslationsTable.state] = translation.stateId
            this[StateTranslationsTable.languageCode] = translation.languageCode
            this[StateTranslationsTable.name] = translation.name
        }
    }

    /**
     * Loads all states or provinces, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All states or provinces mapped to API data objects.
     */
    fun getAllStates(languageCode: String?): List<StateData> = transaction {
        if (languageCode == null) {
            return@transaction StatesTable
                .selectAll()
                .orderBy(StatesTable.name to SortOrder.ASC)
                .map { it.toState(null) }
        }

        return@transaction StatesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(StateTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[StatesTable.id] }
            .map { it.toState(languageCode = languageCode) }
    }

    /**
     * Counts state or province rows for paginated GraphQL listings.
     * @return Total number of states or provinces.
     */
    fun countStates(): Long = transaction {
        StatesTable.selectAll().count()
    }

    /**
     * Counts state or province rows for one country.
     * @param countryId Country primary key used to filter related rows.
     * @return Total number of states or provinces in the country.
     */
    fun countStatesByCountryId(
        countryId: Int,
        languageCode: String?,
        query: String?,
    ): Long = transaction {
        if (query == null) {
            return@transaction StatesTable
                .select(StatesTable.id)
                .where { StatesTable.country eq countryId }
                .count()
        }

        val likeQuery = "%${query.lowercase()}%"

        if (languageCode == null) {
            return@transaction StatesTable
                .select(StatesTable.id)
                .where {
                    (StatesTable.country eq countryId) and
                            (StatesTable.name.lowerCase() like likeQuery)
                }
                .count()
        }

        StatesTable
            .joinTranslations(languageCode)
            .select(StatesTable.id)
            .where {
                (StatesTable.country eq countryId) and
                        (
                                (StatesTable.name.lowerCase() like likeQuery) or
                                        (StateTranslationsTable.name.lowerCase() like likeQuery)
                                )
            }
            .count()
    }

    /**
     * Loads one page of states or provinces, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Zero-based number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Requested state or province page mapped to API data objects.
     */
    fun getStatesPage(
        languageCode: String?,
        offset: Long,
        limit: Int,
    ): List<StateData> = transaction {
        if (languageCode == null) {
            return@transaction StatesTable
                .selectAll()
                .orderBy(StatesTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toState(null) }
        }

        return@transaction StatesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(StateTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
            .map { it.toState(languageCode = languageCode) }
    }

    /**
     * Loads a single state or province by primary key.
     * @param id State or province primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching state or province, or null when it does not exist.
     */
    fun getStateById(id: Long, languageCode: String?): StateData? = transaction {
        val query = StatesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { StatesTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toState(languageCode = languageCode)
    }

    /**
     * Loads states or provinces by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return States or provinces matching the supplied ids.
     */
    fun getStatesByIds(
        ids: List<Long>,
        languageCode: String?
    ): List<StateData> = transaction {
        StatesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { StatesTable.id inList ids }
            .distinctBy { it[StatesTable.id] }
            .map { it.toState(languageCode = languageCode) }
    }

    /**
     * Loads states or provinces grouped by country id for batched GraphQL resolution.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching states or provinces.
     */
    fun getStatesByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<StateData>> = transaction {
        val states = if (languageCode == null) {
            StatesTable
        } else {
            StatesTable.joinTranslations(languageCode)
        }

        states
            .selectAll()
            .where { StatesTable.country inList ids }
            .groupBy(
                keySelector = { it[StatesTable.country].value },
                valueTransform = { row -> row.toState(languageCode = languageCode) }
            )
            .mapValues { (_, statesForCountry) ->
                statesForCountry.distinctBy { it.id }
            }
    }

    /**
     * Loads states or provinces for a single country.
     * @param id Country primary key used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return States or provinces in the supplied country.
     */
    fun getStatesByCountryId(id: Int, languageCode: String?): List<StateData> =
        transaction {
            StatesTable
                .joinTranslations(languageCode)
                .innerJoin(CountriesTable)
                .select(StatesTable.columns)
                .where { CountriesTable.id eq id }
                .distinctBy { it[StatesTable.id] }
                .map { it.toState(languageCode = languageCode) }
        }

    /**
     * Loads one page of states or provinces for a single country.
     * @param countryId Country primary key used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Zero-based number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Requested country state page mapped to API data objects.
     */
    fun getStatesPageByCountryId(
        countryId: Int,
        languageCode: String?,
        query: String?,
        offset: Long,
        limit: Int,
    ): List<StateData> = transaction {
        val likeQuery = query?.lowercase()?.let { "%$it%" }

        if (languageCode == null) {
            return@transaction StatesTable
                .selectAll()
                .where {
                    if (likeQuery == null) {
                        StatesTable.country eq countryId
                    } else {
                        (StatesTable.country eq countryId) and
                                (StatesTable.name.lowerCase() like likeQuery)
                    }
                }
                .orderBy(StatesTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toState(null) }
        }

        return@transaction StatesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where {
                if (likeQuery == null) {
                    StatesTable.country eq countryId
                } else {
                    (StatesTable.country eq countryId) and
                            (
                                    (StatesTable.name.lowerCase() like likeQuery) or
                                            (StateTranslationsTable.name.lowerCase() like likeQuery)
                                    )
                }
            }
            .orderBy(StateTranslationsTable.name to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .map { it.toState(languageCode = languageCode) }
    }
}
