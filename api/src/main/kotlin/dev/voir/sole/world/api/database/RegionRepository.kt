package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.RegionTranslationsTable
import dev.voir.sole.world.api.database.table.RegionsTable
import dev.voir.sole.world.api.database.table.SubregionTranslationsTable
import dev.voir.sole.world.api.database.table.SubregionsTable
import dev.voir.sole.world.api.model.RegionData
import dev.voir.sole.world.api.model.RegionData.Companion.toRegion
import dev.voir.sole.world.api.model.SubregionData
import dev.voir.sole.world.api.model.SubregionData.Companion.toSubregion
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for region and subregion lookup operations. */
@Repository
class RegionRepository {
    /**
     * Checks whether a region row exists.
     * @param id Region primary key.
     * @return True when a region exists for the supplied id.
     */
    fun hasRegion(id: Int): Boolean = transaction {
        RegionsTable
            .select(RegionsTable.id)
            .where { RegionsTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Checks whether a subregion row exists.
     * @param id Subregion primary key.
     * @return True when a subregion exists for the supplied id.
     */
    fun hasSubregion(id: Int): Boolean = transaction {
        SubregionsTable
            .select(SubregionsTable.id)
            .where { SubregionsTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Loads all regions, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All regions mapped to API data objects.
     */
    fun getAllRegions(languageCode: String?): List<RegionData> = transaction {
        if (languageCode == null) {
            return@transaction RegionsTable
                .selectAll()
                .orderBy(RegionsTable.name to SortOrder.ASC)
                .map { it.toRegion(null) }
        }

        return@transaction RegionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(RegionTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[RegionsTable.id] }
            .map { it.toRegion(languageCode = languageCode) }
    }

    /**
     * Counts all region rows.
     * @return Total number of regions.
     */
    fun countRegions(): Long = transaction {
        RegionsTable.selectAll().count()
    }

    /**
     * Loads one page of regions, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Regions in the requested page.
     */
    fun getRegionsPage(languageCode: String?, offset: Long, limit: Int): List<RegionData> =
        transaction {
            if (languageCode == null) {
                return@transaction RegionsTable
                    .selectAll()
                    .orderBy(RegionsTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toRegion(null) }
            }

            return@transaction RegionsTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(RegionTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toRegion(languageCode = languageCode) }
        }

    /**
     * Loads all subregions, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All subregions mapped to API data objects.
     */
    fun getAllSubregions(languageCode: String?): List<SubregionData> = transaction {
        if (languageCode == null) {
            return@transaction SubregionsTable
                .selectAll()
                .orderBy(SubregionsTable.name to SortOrder.ASC)
                .map { it.toSubregion(null) }
        }

        return@transaction SubregionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(SubregionTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[SubregionsTable.id] }
            .map { it.toSubregion(languageCode = languageCode) }
    }

    /**
     * Counts all subregion rows.
     * @return Total number of subregions.
     */
    fun countSubregions(): Long = transaction {
        SubregionsTable.selectAll().count()
    }

    /**
     * Loads one page of subregions, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Subregions in the requested page.
     */
    fun getSubregionsPage(languageCode: String?, offset: Long, limit: Int): List<SubregionData> =
        transaction {
            if (languageCode == null) {
                return@transaction SubregionsTable
                    .selectAll()
                    .orderBy(SubregionsTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toSubregion(null) }
            }

            return@transaction SubregionsTable
                .joinTranslations(languageCode)
                .selectAll()
                .orderBy(SubregionTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toSubregion(languageCode = languageCode) }
        }

    /**
     * Loads a single region by primary key.
     * @param id Region primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching region, or null when it does not exist.
     */
    fun getRegionById(id: Int, languageCode: String?): RegionData? = transaction {
        val query = RegionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { RegionsTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toRegion(languageCode = languageCode)
    }

    /**
     * Loads a single subregion by primary key.
     * @param id Subregion primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching subregion, or null when it does not exist.
     */
    fun getSubregionById(id: Int, languageCode: String?): SubregionData? = transaction {
        val query = SubregionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { SubregionsTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toSubregion(languageCode = languageCode)
    }

    /**
     * Loads regions by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Regions matching the supplied ids.
     */
    fun getRegionsByIds(
        ids: List<Int>,
        languageCode: String?
    ): List<RegionData> = transaction {
        RegionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { RegionsTable.id inList ids }
            .distinctBy { it[RegionsTable.id] }
            .map { it.toRegion(languageCode = languageCode) }
    }

    /**
     * Loads subregions by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Subregions matching the supplied ids.
     */
    fun getSubregionsByIds(
        ids: List<Int>,
        languageCode: String?
    ): List<SubregionData> = transaction {
        SubregionsTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { SubregionsTable.id inList ids }
            .distinctBy { it[SubregionsTable.id] }
            .map { it.toSubregion(languageCode = languageCode) }
    }

    /**
     * Loads subregions belonging to any of the supplied regions.
     * @param ids Region ids used to filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Subregions in the supplied regions.
     */
    fun getSubregionsByRegionIds(
        ids: Set<Int>,
        languageCode: String?
    ): List<SubregionData> = transaction {
        SubregionsTable
            .joinTranslations(languageCode)
            .innerJoin(RegionsTable)
            .selectAll()
            .where { RegionsTable.id inList ids }
            .distinctBy { it[SubregionsTable.id] }
            .map { it.toSubregion(languageCode = languageCode) }
    }

    /**
     * Loads subregions grouped by region id for batched GraphQL resolution.
     * @param ids Region ids used to group related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by region id with matching subregions.
     */
    fun getSubregionsGroupedByRegionIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<SubregionData>> = transaction {
        val subregions = if (languageCode == null) {
            SubregionsTable
        } else {
            SubregionsTable.joinTranslations(languageCode)
        }

        subregions
            .selectAll()
            .where { SubregionsTable.region inList ids }
            .groupBy(
                keySelector = { it[SubregionsTable.region].value },
                valueTransform = { row -> row.toSubregion(languageCode = languageCode) }
            )
            .mapValues { (_, subregionsForRegion) ->
                subregionsForRegion.distinctBy { it.id }
            }
    }
}
