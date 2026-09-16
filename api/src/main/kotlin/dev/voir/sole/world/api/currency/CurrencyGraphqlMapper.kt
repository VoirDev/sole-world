package dev.voir.sole.world.api.currency

import dev.voir.sole.world.api.model.CurrencyData
import dev.voir.sole.world.graphql.dto.types.Currency

/**
 * Maps a currency onto its generated GraphQL type.
 * @return GraphQL currency type.
 */
fun CurrencyData.toGql() = Currency(
    id = id.toString(),
    iso3 = iso3,
    isoNumeric = isoNumeric,
    name = name,
    decimalDigits = decimalDigits,
    popularity = popularity,
    description = description,
    nativeName = nativeName,
    symbol = symbol,
    year = year,
    introducedDate = introducedDate,
    obsolete = obsolete,
    obsoleteAt = obsoleteAt,
    replacedById = replacedById?.toString(),
    flagId = flagId?.toString(),
)
