package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a shared flag and its media assets.
 * @param id Exposed entity identifier for the row.
 */
class FlagEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FlagEntity>(FlagsTable)

    /** Human-readable flag caption. */
    var caption by FlagsTable.caption

    /** Emoji representation of the flag. */
    var emoji by FlagsTable.emoji

    /** Unicode codepoint form of the flag emoji. */
    var emojiU by FlagsTable.emojiU

    /** Raw square media asset foreign key value. */
    var squareAssetId by FlagsTable.squareAsset

    /** Raw wide media asset foreign key value. */
    var wideAssetId by FlagsTable.wideAsset
}
