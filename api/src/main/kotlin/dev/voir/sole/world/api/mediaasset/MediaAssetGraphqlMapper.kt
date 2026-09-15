package dev.voir.sole.world.api.mediaasset

import dev.voir.sole.world.api.model.MediaAssetData
import dev.voir.sole.world.api.model.MediaAssetImageAspectRatioData
import dev.voir.sole.world.api.model.MediaAssetImageFormatsData
import dev.voir.sole.world.api.model.MediaAssetImageSizesData
import dev.voir.sole.world.api.model.MediaAssetTypeData
import dev.voir.sole.world.graphql.dto.types.ImageAsset
import dev.voir.sole.world.graphql.dto.types.ImageAssetAspectRatio
import dev.voir.sole.world.graphql.dto.types.ImageAssetFormats
import dev.voir.sole.world.graphql.dto.types.ImageAssetSizes
import dev.voir.sole.world.graphql.dto.types.MediaAsset
import dev.voir.sole.world.graphql.dto.types.MediaAssetType

/**
 * Maps a media asset onto its generated GraphQL type.
 * @return GraphQL media asset type.
 */
fun MediaAssetData.toGql() = MediaAsset(
    id = id.toString(),
    type = when (type) {
        MediaAssetTypeData.Image -> MediaAssetType.Image
    },
    key = key,
    description = description,
    image = toImageAsset(),
)

private fun MediaAssetData.toImageAsset(): ImageAsset? {
    if (type != MediaAssetTypeData.Image) {
        return null
    }

    val aspectRatio = when (imageAspectRatio) {
        MediaAssetImageAspectRatioData.Wide -> ImageAssetAspectRatio.Wide
        MediaAssetImageAspectRatioData.Square -> ImageAssetAspectRatio.Square
        null -> return null
    }

    val formats = imageFormats ?: return null

    return ImageAsset(aspectRatio = aspectRatio, formats = formats.toGql())
}

/**
 * Maps image format metadata onto its generated GraphQL type.
 * @return GraphQL image formats type.
 */
fun MediaAssetImageFormatsData.toGql() = ImageAssetFormats(
    svg = svg,
    png = png?.toGql(),
    webp = webp?.toGql(),
    jpg = jpg?.toGql(),
)

/**
 * Maps image size metadata onto its generated GraphQL type.
 * @return GraphQL image sizes type.
 */
fun MediaAssetImageSizesData.toGql() = ImageAssetSizes(
    xs = xs,
    sm = sm,
    md = md,
    lg = lg,
    xl = xl,
)
