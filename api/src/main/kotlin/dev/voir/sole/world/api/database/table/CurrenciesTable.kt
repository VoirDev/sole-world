package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.date

/** Stores currency master records and lifecycle metadata. */
object CurrenciesTable : IntIdTable("currencies") {
    /** ISO 3166-1 alpha-3 country code or ISO 4217 alpha code. */
    val iso3 = varchar("iso3", 10).uniqueIndex()

    /** Numeric ISO code represented as text to preserve leading zeros. */
    val isoNumeric = varchar("iso_numeric", 10).uniqueIndex()

    /** Primary display name. */
    val name = varchar("name", 100)

    /** Human-readable description. */
    val description = text("description").nullable()

    /** Name in the native language or script, when available. */
    val nativeName = varchar("native_name", 255).nullable()

    /** Currency symbol, when available. */
    val symbol = varchar("symbol", 10).nullable()

    /** Introduction year, when known. */
    val year = integer("year").nullable()

    /** Date when the currency was introduced, when known. */
    val introducedDate = date("introduced_date").nullable()

    /** Whether the currency is no longer active. */
    val obsolete = bool("obsolete")

    /** Date when the currency became obsolete, when known. */
    val obsoleteAt = date("obsolete_at").nullable()

    /** Replacement currency reference for obsolete currencies. */
    val replacedBy =
        reference("replaced_by", CurrenciesTable, onDelete = ReferenceOption.SET_NULL).nullable()

    /** Shared flag reference. */
    val flag = reference("flag_id", FlagsTable).nullable()

    /** Number of decimal digits used for currency minor units. */
    val decimalDigits = integer("decimal_digits")

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        CurrenciesTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (CurrenciesTranslationsTable.currency eq id) and
                    (if (languageCode != null) (CurrenciesTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
