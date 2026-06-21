package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Media asset record read from bundled seed data.
 * @property id Media asset primary key used during import.
 * @property type Media asset category.
 * @property imageAspectRatio Image aspect ratio category.
 * @property imageFormats Available image renditions by format.
 * @property description Human-readable media asset description.
 */
@Serializable
internal data class MediaAssetJSON(
    val id: Long,
    val type: String,
    val imageAspectRatio: String,
    val imageFormats: ImageFormatsJSON,
    val description: String,
)
