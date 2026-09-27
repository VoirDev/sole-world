package dev.voir.sole.world.api.mediaasset

import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.MediaAssetsApi
import dev.voir.sole.world.openapi.model.MediaAsset
import dev.voir.sole.world.openapi.model.MediaAssetPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for media asset metadata. */
@RestController
class MediaAssetRestController(
    private val mediaAssetStore: MediaAssetStore,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : MediaAssetsApi {
    override fun listMediaAssets(page: Int, size: Int): ResponseEntity<MediaAssetPage> {
        val result = mediaAssetStore.page(RestRequest.pageRequest(page, size))

        return cache.ok(
            MediaAssetPage(
                items = result.items.map { it.toRest() },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getMediaAsset(id: String): ResponseEntity<MediaAsset> {
        val asset = mediaAssetStore.byId(id) ?: throw ResourceNotFoundException.of("Media asset", id)

        return cache.ok(asset.toRest(), request)
    }
}
