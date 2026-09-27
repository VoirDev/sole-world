package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Media asset record read from bundled seed data.
 * @property id Media asset alias: owner kind, key and shape, such as `flag-us-square`.
 * @property type Media asset category.
 * @property key Stable machine key for the asset, also the name its files are stored under.
 * @property imageAspectRatio Image aspect ratio category.
 * @property imageFormats Available image renditions by format.
 * @property description Human-readable media asset description.
 */
@Serializable
data class MediaAssetJSON(
    val id: String,
    val type: String,
    val key: String,
    val imageAspectRatio: String,
    val imageFormats: ImageFormatsJSON,
    val description: String,
)
