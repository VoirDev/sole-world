package dev.voir.sole.world.api.dataset.index

/**
 * Normalizes identifiers for lookup.
 *
 * An identifier is published in one spelling — `US`, `fr`, `Europe/Paris` — but matched regardless
 * of case, so a caller who stored `us` or typed `EUROPE/PARIS` still finds the record. Every store
 * indexes by [of] and looks up by [of], and that is the whole of the rule: no store compares an
 * identifier any other way.
 */
object IdKey {
    /**
     * Returns the key an identifier is indexed and looked up under.
     * @param id Identifier as published, or as a caller supplied it.
     * @return Trimmed, lowercased identifier.
     */
    fun of(id: String): String = id.trim().lowercase()
}
