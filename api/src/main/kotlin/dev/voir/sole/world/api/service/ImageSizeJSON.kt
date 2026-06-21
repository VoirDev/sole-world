package dev.voir.sole.world.api.service

import dev.voir.sole.world.api.model.MediaAssetImageSizesData
import kotlinx.serialization.Serializable

/**
 * Image rendition URLs grouped by display size.
 * @property xs Extra-small rendition URL or path.
 * @property sm Small rendition URL or path.
 * @property md Medium rendition URL or path.
 * @property lg Large rendition URL or path.
 * @property xl Extra-large rendition URL or path.
 */
@Serializable
internal data class ImageSizeJSON(
    val xs: String?,
    val sm: String?,
    val md: String?,
    val lg: String?,
    val xl: String?,
) {
    companion object {
        /**
         * Converts seed image size URLs into the API model used by media asset metadata.
         * @return Media asset image sizes data.
         */
        fun ImageSizeJSON.toData() = MediaAssetImageSizesData(
            xs = xs,
            sm = sm,
            md = md,
            lg = lg,
            xl = xl,
        )
    }
}
