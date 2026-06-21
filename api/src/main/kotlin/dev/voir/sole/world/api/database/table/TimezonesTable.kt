package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq

/** Stores timezone master records and GMT offsets. */
object TimezonesTable : LongIdTable("timezones") {
    /** IANA timezone identifier. */
    val zoneName = varchar("zone_name", 100)

    /** Display timezone name. */
    val tzName = varchar("tz_name", 100)

    /** Short timezone abbreviation. */
    val abbreviation = varchar("abbreviation", 10)

    /** GMT offset in seconds. */
    val gmtOffset = integer("gmt_offset")

    /** Formatted GMT offset label. */
    val gmtOffsetName = varchar("gmt_offset_name", 20)

    /**
     * Builds a left join to the translation table, optionally constrained by locale.
     * @param languageCode Locale code used to select a translation; joins all translations when null.
     * @return Join expression with translation rows attached.
     */
    fun joinTranslations(languageCode: String?) = this.join(
        TimezoneTranslationsTable,
        JoinType.LEFT,
        additionalConstraint = {
            (TimezoneTranslationsTable.timezone eq id) and
                    (if (languageCode != null) (TimezoneTranslationsTable.languageCode eq languageCode) else Op.TRUE)
        },
    )
}
