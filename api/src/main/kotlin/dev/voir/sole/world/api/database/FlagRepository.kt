package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.FlagsTable
import dev.voir.sole.world.api.model.FlagData
import dev.voir.sole.world.api.model.FlagData.Companion.toFlag
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for shared flag lookup operations. */
@Repository
class FlagRepository {
    /**
     * Loads all flags ordered by caption.
     * @return All flags mapped to API data objects.
     */
    fun getAllFlags(): List<FlagData> = transaction {
        FlagsTable
            .selectAll()
            .orderBy(FlagsTable.caption to SortOrder.ASC)
            .map { it.toFlag() }
    }

    /**
     * Counts all flag rows.
     * @return Total number of flags.
     */
    fun countFlags(): Long = transaction {
        FlagsTable.selectAll().count()
    }

    /**
     * Loads one page of flags ordered by caption.
     * @param offset Number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Flags in the requested page.
     */
    fun getFlagsPage(offset: Long, limit: Int): List<FlagData> = transaction {
        FlagsTable
            .selectAll()
            .orderBy(FlagsTable.caption to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .map { it.toFlag() }
    }

    /**
     * Loads flags by primary key.
     * @param ids Primary keys to load.
     * @return Flags matching the supplied ids.
     */
    fun getFlagsByIds(ids: List<Int>): List<FlagData> = transaction {
        FlagsTable
            .selectAll()
            .where { FlagsTable.id inList ids }
            .map { it.toFlag() }
    }
}
