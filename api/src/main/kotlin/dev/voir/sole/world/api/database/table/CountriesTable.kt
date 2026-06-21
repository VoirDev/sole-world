package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores country master records and geography metadata. */
object CountriesTable : IntIdTable("countries") {
    /** Primary display name. */
    val name = varchar("name", 100)

    /** Name in the native language or script, when available. */
    val nativeName = varchar("native_name", 100).nullable()

    /** ISO 3166-1 alpha-2 country code. */
    val iso2 = varchar("iso2", 2).uniqueIndex()

    /** ISO 3166-1 alpha-3 country code or ISO 4217 alpha code. */
    val iso3 = varchar("iso3", 3).uniqueIndex()

    /** Numeric ISO code represented as text to preserve leading zeros. */
    val isoNumeric = varchar("iso_numeric", 3).uniqueIndex()

    /** International calling code. */
    val phoneCode = varchar("phone_code", 5)

    /** Country top-level domain, when available. */
    val tld = varchar("tld", 10).nullable()

    /** Latitude coordinate in decimal degrees. */
    val latitude = double("latitude")

    /** Longitude coordinate in decimal degrees. */
    val longitude = double("longitude")

    /** Shared flag reference. */
    val flag = reference("flag_id", FlagsTable).nullable()

    /** Referenced parent region record. */
    val region = reference("region_id", RegionsTable)

    /** Referenced parent subregion record. */
    val subregion = reference("subregion_id", SubregionsTable)

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = join(
        CountryTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (CountryTranslationsTable.country eq id) and
                    (if (languageCode != null) (CountryTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
