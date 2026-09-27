package dev.voir.sole.world.api.model

/**
 * API model returned for media asset records.
 * @property id Media asset alias: owner kind, key and shape, such as `flag-us-square`.
 * @property type Top-level media asset category.
 * @property key Stable machine key for the asset. Callers that need to recognise an asset match on
 * this; [description] is prose for people.
 * @property imageAspectRatio Aspect-ratio category for image assets.
 * @property imageFormats Available image URLs grouped by format.
 * @property description Human-readable media asset description.
 */
data class MediaAssetData(
    val id: String,
    val type: MediaAssetTypeData,
    val key: String,
    val imageAspectRatio: MediaAssetImageAspectRatioData?,
    val imageFormats: MediaAssetImageFormatsData?,
    val description: String,
)
