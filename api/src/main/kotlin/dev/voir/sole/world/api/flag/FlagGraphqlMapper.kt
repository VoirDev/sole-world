package dev.voir.sole.world.api.flag

import dev.voir.sole.world.api.model.FlagData
import dev.voir.sole.world.graphql.dto.types.Flag

/**
 * Maps a flag onto its generated GraphQL type.
 * @return GraphQL flag type.
 */
fun FlagData.toGql() = Flag(
    id = id,
    caption = caption,
    emoji = emoji,
    emojiU = emojiU,
    squareAssetId = squareAssetId,
    wideAssetId = wideAssetId,
)
