package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores world subregions belonging to regions. */
object SubregionsTable : IntIdTable("subregions") {
    /** Primary display name. */
    val name = varchar("name", 255)

    /** Referenced parent region record. */
    val region = reference("region_id", RegionsTable)

    /** Wikidata identifier for external lookup. */
    val wikiDataId = varchar("wiki_data_id", 100)

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        SubregionTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (SubregionTranslationsTable.subregion eq id) and
                    (if (languageCode != null) (SubregionTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
