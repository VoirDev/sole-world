package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores central bank master records. */
object CentralBanksTable : IntIdTable("central_banks") {
    /** Primary display name. */
    val name = varchar("name", 100)

    /** Name in the native language or script, when available. */
    val nativeName = varchar("native_name", 255).nullable()

    /** Official website URL, when available. */
    val websiteURL = varchar("website_url", 128).nullable()

    /** Year the central bank was established, when known. */
    val establishmentYear = integer("establishment_year").nullable()

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        CentralBankTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (CentralBankTranslationsTable.centralBank eq id) and
                    (if (languageCode != null) (CentralBankTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
