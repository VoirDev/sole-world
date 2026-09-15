package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Available image format URLs for a media asset in seed data.
 * @property svg SVG asset URL or path, when available.
 * @property png PNG renditions by size, when available.
 * @property webp WebP renditions by size, when available.
 * @property jpg JPEG renditions by size, when available.
 */
@Serializable
data class ImageFormatsJSON(
    val svg: String?,
    val png: ImageSizeJSON?,
    val webp: ImageSizeJSON?,
    val jpg: ImageSizeJSON?,
)
