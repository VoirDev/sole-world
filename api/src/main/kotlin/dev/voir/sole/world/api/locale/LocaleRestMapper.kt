package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.language.toRest
import dev.voir.sole.world.api.model.LocaleData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.Language
import dev.voir.sole.world.openapi.model.Locale
import org.springframework.stereotype.Component

/**
 * Maps a locale onto its published REST type.
 * @return REST locale with any supplied relationships embedded.
 */
fun LocaleData.toRest(
    language: Language? = null,
    truncatedIncludes: List<String>? = null,
) = Locale(
    id = id,
    languageId = languageId,
    name = name,
    nativeName = nativeName,
    language = language,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST locales, resolving the relationships a caller asked to embed. */
@Component
class LocaleRestAssembler(
    private val languageStore: LanguageStore,
) {
    /**
     * Assembles one locale.
     * @param data Locale read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST locale with the requested relationships embedded.
     */
    fun assemble(data: LocaleData, includes: IncludeSpec, languageCode: String?): Locale {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val language =
            assembler.one(LANGUAGE, { languageStore.byId(data.languageId, languageCode) }) { it.toRest() }

        return data.toRest(
            language = language,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val LANGUAGE = "language"

        /** Relationships a locale endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(LANGUAGE)
    }
}
