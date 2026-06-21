package dev.voir.sole.world.api.database.table

import dev.voir.sole.world.api.model.MediaAssetImageFormatsData
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.json

/** JSON format used for serializing media asset image metadata. */
private val format = Json { prettyPrint = true }

/** Stores reusable media asset metadata, including image formats. */
object MediaAssetsTable : LongIdTable("media_assets") {
    /** Classification value for this record. */
    val type = varchar("type", 10) // image

    /** Aspect ratio category for image assets. */
    val imageAspectRatio = varchar("image_aspect_ratio", 10).nullable() // square, wide

    /** Available image renditions keyed by format or size. */
    val imageFormats = json<MediaAssetImageFormatsData>("image_formats", format).nullable()

    /** Human-readable description. */
    val description = text("description")
}
