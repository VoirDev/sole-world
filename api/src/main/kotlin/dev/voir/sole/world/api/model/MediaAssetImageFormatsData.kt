package dev.voir.sole.world.api.model

import kotlinx.serialization.Serializable

/**
 * Serializable media asset image URLs grouped by file format.
 * @property svg SVG asset URL or path, when available.
 * @property png PNG renditions by size, when available.
 * @property webp WebP renditions by size, when available.
 * @property jpg JPEG renditions by size, when available.
 */
@Serializable
data class MediaAssetImageFormatsData(
    val svg: String?,
    val png: MediaAssetImageSizesData?,
    val webp: MediaAssetImageSizesData?,
    val jpg: MediaAssetImageSizesData?,
)
