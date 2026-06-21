package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/** Stores shared flag metadata and associated media assets. */
object FlagsTable : IntIdTable("flags") {
    /** Human-readable flag caption. */
    val caption = varchar("caption", 255)

    /** Emoji representation of the flag. */
    val emoji = varchar("emoji", 16)

    /** Unicode codepoint form of the flag emoji. */
    val emojiU = varchar("emoji_u", 64)

    /** Square flag media asset reference. */
    val squareAsset = reference("square_asset_id", MediaAssetsTable).nullable()

    /** Wide flag media asset reference. */
    val wideAsset = reference("wide_asset_id", MediaAssetsTable).nullable()
}
