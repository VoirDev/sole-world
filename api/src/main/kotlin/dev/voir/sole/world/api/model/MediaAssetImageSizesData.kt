package dev.voir.sole.world.api.model

import kotlinx.serialization.Serializable

/**
 * Serializable media asset image URLs grouped by display size.
 * @property xs Extra-small rendition URL or path.
 * @property sm Small rendition URL or path.
 * @property md Medium rendition URL or path.
 * @property lg Large rendition URL or path.
 * @property xl Extra-large rendition URL or path.
 */
@Serializable
data class MediaAssetImageSizesData(
    val xs: String?,
    val sm: String?,
    val md: String?,
    val lg: String?,
    val xl: String?,
)
