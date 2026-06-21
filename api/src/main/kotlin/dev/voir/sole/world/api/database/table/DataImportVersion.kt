package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/** Tracks the imported data set version applied to the database. */
object DataImportVersion : IntIdTable("data_import_version") {
    /** Imported data set version number. */
    val version = integer("version")
}
