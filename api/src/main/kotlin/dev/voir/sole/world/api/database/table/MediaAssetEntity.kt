package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity for a media asset and its image metadata.
 * @param id Exposed entity identifier for the row.
 */
class MediaAssetEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MediaAssetEntity>(MediaAssetsTable)

    /** Classification value for this record. */
    var type by MediaAssetsTable.type

    /** Aspect ratio category for image assets. */
    var imageAspectRatio by MediaAssetsTable.imageAspectRatio

    /** Available image renditions keyed by format or size. */
    var imageFormats by MediaAssetsTable.imageFormats

    /** Human-readable description. */
    var description by MediaAssetsTable.description
}
