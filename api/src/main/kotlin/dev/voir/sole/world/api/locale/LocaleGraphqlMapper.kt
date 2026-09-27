package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.model.LocaleData
import dev.voir.sole.world.graphql.dto.types.Locale

/**
 * Maps a locale onto its generated GraphQL type.
 * @return GraphQL locale type.
 */
fun LocaleData.toGql() = Locale(
    id = id,
    languageId = languageId,
    name = name,
    nativeName = nativeName,
)
