package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.MediaAssetsTable
import dev.voir.sole.world.api.model.MediaAssetData
import dev.voir.sole.world.api.model.MediaAssetData.Companion.toMediaAsset
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for media asset lookup operations. */
@Repository
class MediaAssetRepository {
    /**
     * Loads all media assets ordered by id.
     * @return All media assets mapped to API data objects.
     */
    fun getAllMediaAssets(): List<MediaAssetData> = transaction {
        MediaAssetsTable
            .selectAll()
            .orderBy(MediaAssetsTable.id to SortOrder.ASC)
            .map { it.toMediaAsset() }
    }

    /**
     * Counts media asset rows for paginated GraphQL listings.
     * @return Total number of media assets.
     */
    fun countMediaAssets(): Long = transaction {
        MediaAssetsTable.selectAll().count()
    }

    /**
     * Loads one page of media assets ordered by id.
     * @param offset Zero-based number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Requested media asset page mapped to API data objects.
     */
    fun getMediaAssetsPage(offset: Long, limit: Int): List<MediaAssetData> = transaction {
        MediaAssetsTable
            .selectAll()
            .orderBy(MediaAssetsTable.id to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .map { it.toMediaAsset() }
    }

    /**
     * Loads a single media asset by primary key.
     * @param id Media asset primary key.
     * @return The matching media asset, or null when it does not exist.
     */
    fun getMediaAssetById(id: Long): MediaAssetData? = transaction {
        MediaAssetsTable
            .selectAll()
            .where { MediaAssetsTable.id eq id }
            .firstOrNull()
            ?.toMediaAsset()
    }

    /**
     * Loads media assets by primary key.
     * @param ids Primary keys to load.
     * @return Media assets matching the supplied ids.
     */
    fun getMediaAssetsByIds(
        ids: List<Long>,
    ): List<MediaAssetData> = transaction {
        MediaAssetsTable
            .selectAll()
            .where { MediaAssetsTable.id inList ids }
            .map { it.toMediaAsset() }
    }
}
