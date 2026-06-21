package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.CountryTimezonesTable
import dev.voir.sole.world.api.database.table.TimezoneTranslationsTable
import dev.voir.sole.world.api.database.table.TimezonesTable
import dev.voir.sole.world.api.model.TimezoneData
import dev.voir.sole.world.api.model.TimezoneData.Companion.toTimezone
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for timezone lookup operations and country-timezone grouping queries. */
@Repository
class TimezoneRepository {
    /**
     * Checks whether a timezone row exists.
     * @param id Timezone primary key.
     * @return True when a timezone exists for the supplied id.
     */
    fun hasTimezone(id: Long): Boolean = transaction {
        TimezonesTable
            .select(TimezonesTable.id)
            .where { TimezonesTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Loads all timezones, ordered by localized display name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All timezones mapped to API data objects.
     */
    fun getAllTimezones(languageCode: String?): List<TimezoneData> = transaction {
        if (languageCode == null) {
            return@transaction TimezonesTable
                .selectAll()
                .orderBy(TimezonesTable.tzName to SortOrder.ASC)
                .map { it.toTimezone(null) }
        }

        return@transaction TimezonesTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(TimezoneTranslationsTable.tzName to SortOrder.ASC)
            .distinctBy { it[TimezonesTable.id] }
            .map { it.toTimezone(languageCode = languageCode) }
    }

    /**
     * Counts all timezone rows.
     * @return Total number of timezones.
     */
    fun countTimezones(): Long = transaction {
        TimezonesTable.selectAll().count()
    }

    /**
     * Loads one page of timezones, ordered by localized display name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Timezones in the requested page.
     */
    fun getTimezonesPage(languageCode: String?, offset: Long, limit: Int): List<TimezoneData> =
        transaction {
            if (languageCode == null) {
                return@transaction TimezonesTable
                    .selectAll()
                    .orderBy(TimezonesTable.tzName to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toTimezone(null) }
            }

            return@transaction TimezonesTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(TimezoneTranslationsTable.tzName to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toTimezone(languageCode = languageCode) }
        }

    /**
     * Loads a single timezone by primary key.
     * @param id Timezone primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching timezone, or null when it does not exist.
     */
    fun getTimezoneById(id: Long, languageCode: String?): TimezoneData? = transaction {
        val query = TimezonesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { TimezonesTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toTimezone(languageCode = languageCode)
    }

    /**
     * Loads timezones by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Timezones matching the supplied ids.
     */
    fun getTimezonesByIds(
        ids: List<Long>,
        languageCode: String?
    ): List<TimezoneData> = transaction {
        TimezonesTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { TimezonesTable.id inList ids }
            .distinctBy { it[TimezonesTable.id] }
            .map { it.toTimezone(languageCode = languageCode) }
    }

    /**
     * Loads timezones grouped by country id for batched GraphQL resolution.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching timezones.
     */
    fun getTimezonesByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<TimezoneData>> = transaction {
        val timezones = if (languageCode == null) {
            TimezonesTable
        } else {
            TimezonesTable.joinTranslations(languageCode)
        }

        timezones
            .innerJoin(CountryTimezonesTable)
            .selectAll()
            .where { CountryTimezonesTable.country inList ids }
            .groupBy(
                keySelector = { it[CountryTimezonesTable.country].value },
                valueTransform = { row -> row.toTimezone(languageCode) }
            )
            .mapValues { (_, timezonesForCountry) ->
                timezonesForCountry.distinctBy { it.id }
            }
    }
}
