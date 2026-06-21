package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores language master records and optional flag assets. */
object LanguagesTable : IntIdTable("languages") {
    /** ISO or application language code. */
    val code = varchar("code", 10).uniqueIndex()

    /** Name in the native language or script, when available. */
    val nativeName = varchar("native_name", 100).nullable()

    /** Primary display name. */
    val name = varchar("name", 100)

    /** Human-readable description. */
    val description = text("description").nullable()

    /** Shared flag reference. */
    val flag = reference("flag_id", FlagsTable).nullable()

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        LanguageTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (LanguageTranslationsTable.language eq id) and
                    (if (languageCode != null) (LanguageTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
