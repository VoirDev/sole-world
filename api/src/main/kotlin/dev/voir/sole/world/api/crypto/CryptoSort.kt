package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.dataset.index.SortOrder

/**
 * Fields a cryptocurrency listing can be ordered by.
 *
 * Popularity is the default for the same reason it is for currencies: an alphabetical first page
 * of 250 coins opens on tokens nobody is looking for and never reaches Bitcoin. The other fields
 * are here for the cases it is not what is wanted — a reference table wants names, a lookup wants
 * tickers, a timeline wants launch years.
 *
 * @property wireName Spelling used by the `sort` query parameter and the GraphQL enum's lowercase form.
 * @property defaultOrder Direction used when the caller does not ask for one.
 */
enum class CryptoSort(val wireName: String, val defaultOrder: SortOrder) {
    /** Most widely held and traded first. */
    POPULARITY("popularity", SortOrder.DESC),

    /** Display name, collated for the requested language. */
    NAME("name", SortOrder.ASC),

    /** Ticker symbol. */
    CODE("code", SortOrder.ASC),

    /** Launch year, oldest first; coins with no known year come last either way. */
    YEAR("year", SortOrder.ASC),
}
