package dev.voir.sole.world.api.mediaasset

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.json.ImageFormatsJSON
import dev.voir.sole.world.api.dataset.json.ImageSizeJSON
import dev.voir.sole.world.api.dataset.json.MediaAssetJSON
import dev.voir.sole.world.api.model.MediaAssetData
import dev.voir.sole.world.api.model.MediaAssetImageAspectRatioData
import dev.voir.sole.world.api.model.MediaAssetImageFormatsData
import dev.voir.sole.world.api.model.MediaAssetImageSizesData
import dev.voir.sole.world.api.model.MediaAssetTypeData
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Read model for media asset metadata. Media assets carry no translated fields.
 *
 * Paths are published in their caller-ready form here rather than at the edge, so every route that
 * can hand out an asset — the media asset endpoints, a flag's renditions, a coin's logo, on either
 * transport — publishes the same thing, and the work is done once at startup.
 */
@Component
@EnableConfigurationProperties(AssetUrlProperties::class)
class MediaAssetStore(dataset: RawDataset, assetUrls: AssetUrlProperties) {
    private val assetUrl = AssetUrl(assetUrls.baseUrl)

    private val ordered: List<MediaAssetData> = dataset.mediaAssets
        .map(::toMediaAsset)
        .sortedBy { it.id }

    private val byId: IdIndex<MediaAssetData> = IdIndex.of(ordered) { it.id }

    /** Total number of media assets. */
    val size: Int get() = ordered.size

    /**
     * Loads one media asset.
     * @param id Media asset identifier.
     * @return Matching media asset, or null when none exists.
     */
    fun byId(id: String): MediaAssetData? = byId[id]

    /**
     * Loads several media assets, skipping identifiers that do not exist.
     * @param ids Media asset identifiers.
     * @return Matching media assets in request order.
     */
    fun byIds(ids: List<String>): List<MediaAssetData> = ids.mapNotNull(byId::get)

    /**
     * Returns one page of media assets ordered by identifier.
     * @param request Normalized page request.
     * @return Requested page.
     */
    fun page(request: PageRequest): Page<MediaAssetData> = ordered.toPage(request)

    private fun toMediaAsset(json: MediaAssetJSON) = MediaAssetData(
        id = json.id,
        type = when (json.type) {
            "image" -> MediaAssetTypeData.Image
            else -> throw IllegalStateException("Unknown media asset type '${json.type}' on asset ${json.id}")
        },
        key = json.key,
        imageAspectRatio = when (json.imageAspectRatio) {
            "square" -> MediaAssetImageAspectRatioData.Square
            "wide" -> MediaAssetImageAspectRatioData.Wide
            else -> null
        },
        imageFormats = toFormats(json.imageFormats),
        description = json.description,
    )

    private fun toFormats(json: ImageFormatsJSON) = MediaAssetImageFormatsData(
        svg = assetUrl.of(json.svg),
        png = json.png?.let(::toSizes),
        webp = json.webp?.let(::toSizes),
        jpg = json.jpg?.let(::toSizes),
    )

    private fun toSizes(json: ImageSizeJSON) = MediaAssetImageSizesData(
        xs = assetUrl.of(json.xs),
        sm = assetUrl.of(json.sm),
        md = assetUrl.of(json.md),
        lg = assetUrl.of(json.lg),
        xl = assetUrl.of(json.xl),
    )
}
