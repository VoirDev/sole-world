package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Media asset record read from bundled seed data.
 * @property id Media asset primary key used during import.
 * @property type Media asset category.
 * @property key Stable machine key for the asset, also the name its files are stored under.
 * @property imageAspectRatio Image aspect ratio category.
 * @property imageFormats Available image renditions by format.
 * @property description Human-readable media asset description.
 */
@Serializable
data class MediaAssetJSON(
    val id: Long,
    val type: String,
    val key: String,
    val imageAspectRatio: String,
    val imageFormats: ImageFormatsJSON,
    val description: String,
)
