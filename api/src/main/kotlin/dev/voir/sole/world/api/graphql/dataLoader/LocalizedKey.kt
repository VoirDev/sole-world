package dev.voir.sole.world.api.graphql.dataLoader

import dev.voir.sole.world.api.graphql.PageRequest

/** DataLoader key for localized Int-id lookups. */
data class LocalizedIntKey(
    val id: Int,
    val languageCode: String?,
)

/** DataLoader key for localized, paginated country relationship lookups. */
data class LocalizedCountryPageKey(
    val countryId: Int,
    val languageCode: String?,
    val pageRequest: PageRequest,
    val query: String?,
)
