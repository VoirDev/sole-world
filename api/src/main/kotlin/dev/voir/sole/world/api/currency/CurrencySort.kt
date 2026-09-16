package dev.voir.sole.world.api.currency

import dev.voir.sole.world.api.dataset.index.SortOrder

/**
 * Fields a currency listing can be ordered by.
 *
 * Every other collection in this API is ordered by localized name, which for currencies buries the
 * handful anyone actually asks about: an alphabetical first page of the 155 records runs from the
 * Afghan afghani to the Bahraini dinar without reaching the dollar or the euro. Popularity is what
 * a picker wants, so it is the default, and the other fields are here for the cases it is not —
 * a reference table wants names, a lookup wants codes, a timeline wants years.
 *
 * @property wireName Spelling used by the `sort` query parameter and the GraphQL enum's lowercase form.
 * @property defaultOrder Direction used when the caller does not ask for one.
 */
enum class CurrencySort(val wireName: String, val defaultOrder: SortOrder) {
    /** Most widely used first. */
    POPULARITY("popularity", SortOrder.DESC),

    /** Localized display name, collated for the requested language. */
    NAME("name", SortOrder.ASC),

    /** ISO 4217 alpha code. */
    CODE("code", SortOrder.ASC),

    /** Introduction year, oldest first; currencies with no known year come last either way. */
    YEAR("year", SortOrder.ASC),
}
