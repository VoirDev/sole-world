package dev.voir.sole.world.api.language

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toRest
import dev.voir.sole.world.api.model.LanguageData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Flag
import dev.voir.sole.world.openapi.model.Language
import org.springframework.stereotype.Component

/**
 * Maps a language onto its published REST type.
 * @return REST language with any supplied relationships embedded.
 */
fun LanguageData.toRest(
    flag: Flag? = null,
    countries: List<Country>? = null,
    truncatedIncludes: List<String>? = null,
) = Language(
    id = id,
    code = code,
    name = name,
    nativeName = nativeName,
    description = description,
    flagId = flagId,
    flag = flag,
    countries = countries,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST languages, resolving the relationships a caller asked to embed. */
@Component
class LanguageRestAssembler(
    private val countryStore: CountryStore,
    private val flagStore: FlagStore,
) {
    /**
     * Assembles one language.
     * @param data Language read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST language with the requested relationships embedded.
     */
    fun assemble(data: LanguageData, includes: IncludeSpec, languageCode: String?): Language {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val flag = assembler.one(FLAG, { data.flagId?.let(flagStore::byId) }) { it.toRest() }
        val countries =
            assembler.many(COUNTRIES, { countryStore.byLanguageId(data.id, languageCode) }) { it.toRest() }

        return data.toRest(
            flag = flag,
            countries = countries,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val FLAG = "flag"
        const val COUNTRIES = "countries"

        /** Relationships a language endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(FLAG, COUNTRIES)
    }
}
