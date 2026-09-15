package dev.voir.sole.world.api.language

import dev.voir.sole.world.api.model.LanguageData
import dev.voir.sole.world.graphql.dto.types.Language

/**
 * Maps a language onto its generated GraphQL type.
 * @return GraphQL language type.
 */
fun LanguageData.toGql() = Language(
    id = id.toString(),
    code = code,
    nativeName = nativeName,
    name = name,
    description = description,
    flagId = flagId?.toString(),
)
