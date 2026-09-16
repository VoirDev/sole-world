package dev.voir.sole.world.api.flag

import dev.voir.sole.world.api.mediaasset.MediaAssetStore
import dev.voir.sole.world.api.mediaasset.toRest
import dev.voir.sole.world.api.model.FlagData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.Flag
import dev.voir.sole.world.openapi.model.MediaAsset
import org.springframework.stereotype.Component

/**
 * Maps a flag onto its published REST type.
 * @return REST flag.
 */
fun FlagData.toRest(
    squareAsset: MediaAsset? = null,
    wideAsset: MediaAsset? = null,
) = Flag(
    id = id,
    caption = caption,
    emoji = emoji,
    emojiU = emojiU,
    squareAssetId = squareAssetId,
    wideAssetId = wideAssetId,
    squareAsset = squareAsset,
    wideAsset = wideAsset,
)

/** Builds REST flags, resolving the media assets a caller asked to embed. */
@Component
class FlagRestAssembler(
    private val mediaAssetStore: MediaAssetStore,
) {
    /**
     * Assembles one flag.
     * @param data Flag read from the store.
     * @param includes Relationships the caller asked for.
     * @return REST flag with the requested relationships embedded.
     */
    fun assemble(data: FlagData, includes: IncludeSpec): Flag {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        return data.toRest(
            squareAsset = assembler.one(
                SQUARE_ASSET,
                { data.squareAssetId?.let(mediaAssetStore::byId) },
                { it.toRest() },
            ),
            wideAsset = assembler.one(
                WIDE_ASSET,
                { data.wideAssetId?.let(mediaAssetStore::byId) },
                { it.toRest() },
            ),
        )
    }

    companion object {
        const val SQUARE_ASSET = "squareAsset"
        const val WIDE_ASSET = "wideAsset"

        /** Relationships a flag endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(SQUARE_ASSET, WIDE_ASSET)
    }
}
