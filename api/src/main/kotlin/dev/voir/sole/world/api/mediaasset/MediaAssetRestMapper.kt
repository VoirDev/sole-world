package dev.voir.sole.world.api.mediaasset

import dev.voir.sole.world.api.model.MediaAssetData
import dev.voir.sole.world.api.model.MediaAssetImageAspectRatioData
import dev.voir.sole.world.api.model.MediaAssetImageFormatsData
import dev.voir.sole.world.api.model.MediaAssetImageSizesData
import dev.voir.sole.world.api.model.MediaAssetTypeData
import dev.voir.sole.world.openapi.model.ImageAsset
import dev.voir.sole.world.openapi.model.ImageAssetFormats
import dev.voir.sole.world.openapi.model.ImageAssetSizes
import dev.voir.sole.world.openapi.model.MediaAsset

/**
 * Maps a media asset onto its published REST type.
 * @return REST media asset.
 */
fun MediaAssetData.toRest() = MediaAsset(
    id = id,
    type = when (type) {
        MediaAssetTypeData.Image -> MediaAsset.Type.IMAGE
    },
    key = key,
    description = description,
    image = toImageAsset(),
)

private fun MediaAssetData.toImageAsset(): ImageAsset? {
    val ratio = when (imageAspectRatio) {
        MediaAssetImageAspectRatioData.Square -> ImageAsset.AspectRatio.SQUARE
        MediaAssetImageAspectRatioData.Wide -> ImageAsset.AspectRatio.WIDE
        null -> return null
    }

    val formats = imageFormats ?: return null

    return ImageAsset(aspectRatio = ratio, formats = formats.toRest())
}

/**
 * Maps image format metadata onto its published REST type.
 * @return REST image formats.
 */
fun MediaAssetImageFormatsData.toRest() = ImageAssetFormats(
    svg = svg,
    png = png?.toRest(),
    webp = webp?.toRest(),
    jpg = jpg?.toRest(),
)

/**
 * Maps image size metadata onto its published REST type.
 * @return REST image sizes.
 */
fun MediaAssetImageSizesData.toRest() = ImageAssetSizes(xs = xs, sm = sm, md = md, lg = lg, xl = xl)
