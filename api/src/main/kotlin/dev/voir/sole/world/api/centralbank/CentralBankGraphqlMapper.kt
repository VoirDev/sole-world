package dev.voir.sole.world.api.centralbank

import dev.voir.sole.world.api.model.CentralBankData
import dev.voir.sole.world.graphql.dto.types.CentralBank

/**
 * Maps a central bank onto its generated GraphQL type.
 * @return GraphQL central bank type.
 */
fun CentralBankData.toGql() = CentralBank(
    id = id.toString(),
    name = name,
    nativeName = nativeName,
    websiteUrl = websiteURL,
    establishmentYear = establishmentYear,
)
