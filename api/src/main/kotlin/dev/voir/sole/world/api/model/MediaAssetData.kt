package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.MediaAssetsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for media asset records.
 * @property id Primary key for the record.
 * @property type Top-level media asset category.
 * @property imageAspectRatio Aspect-ratio category for image assets.
 * @property imageFormats Available image URLs grouped by format.
 * @property description Human-readable media asset description.
 */
data class MediaAssetData(
    val id: Long,
    val type: MediaAssetTypeData,
    val imageAspectRatio: MediaAssetImageAspectRatioData?,
    val imageFormats: MediaAssetImageFormatsData?,
    val description: String,
) {
    companion object {
        /**
         * Maps an Exposed result row to a media asset API model.
         * @return Media asset API model populated from this row.
         */
        fun ResultRow.toMediaAsset() = MediaAssetData(
            id = this[MediaAssetsTable.id].value,
            type = when (this[MediaAssetsTable.type]) {
                "image" -> MediaAssetTypeData.Image
                else -> throw Exception("Invalid media asset type: ${this[MediaAssetsTable.type]}")
            },
            imageAspectRatio = when (this[MediaAssetsTable.imageAspectRatio]) {
                "wide" -> MediaAssetImageAspectRatioData.Wide
                "square" -> MediaAssetImageAspectRatioData.Square
                else -> null
            },
            imageFormats = this[MediaAssetsTable.imageFormats],
            description = this[MediaAssetsTable.description],
        )
    }
}
