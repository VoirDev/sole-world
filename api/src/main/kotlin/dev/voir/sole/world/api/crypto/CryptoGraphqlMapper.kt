package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.model.CryptoData
import dev.voir.sole.world.graphql.dto.types.Crypto

/**
 * Maps a cryptocurrency onto its generated GraphQL type.
 * @return GraphQL cryptocurrency type.
 */
fun CryptoData.toGql() = Crypto(
    id = id.toString(),
    code = code,
    alias = alias,
    name = name,
    description = description,
    websiteUrl = websiteUrl,
    introducedYear = introducedYear,
    decimalDigits = decimalDigits,
    obsolete = obsolete,
    obsoleteAt = obsoleteAt,
    logoId = logoId?.toString(),
)
