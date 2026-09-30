package dev.voir.sole.world.api.country

import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.centralbank.toRest
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.currency.toRest
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toRest
import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.language.toRest
import dev.voir.sole.world.api.model.CountryData
import dev.voir.sole.world.api.model.StateData
import dev.voir.sole.world.api.region.RegionStore
import dev.voir.sole.world.api.region.toRest
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.state.toRest
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.api.subregion.toRest
import dev.voir.sole.world.api.timezone.TimezoneStore
import dev.voir.sole.world.api.timezone.toRest
import dev.voir.sole.world.openapi.model.CentralBank
import dev.voir.sole.world.openapi.model.Coordinates
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Currency
import dev.voir.sole.world.openapi.model.Flag
import dev.voir.sole.world.openapi.model.Language
import dev.voir.sole.world.openapi.model.Region
import dev.voir.sole.world.openapi.model.State
import dev.voir.sole.world.openapi.model.Subregion
import dev.voir.sole.world.openapi.model.Timezone
import org.springframework.stereotype.Component

/**
 * Maps a country onto its published REST type.
 * @param aliases The country's aliases, when the caller asked for them.
 * @return REST country with any supplied relationships embedded.
 */
fun CountryData.toRest(
    aliases: List<String>? = null,
    region: Region? = null,
    subregion: Subregion? = null,
    flag: Flag? = null,
    currencies: List<Currency>? = null,
    languages: List<Language>? = null,
    timezones: List<Timezone>? = null,
    centralBanks: List<CentralBank>? = null,
    states: List<State>? = null,
    truncatedIncludes: List<String>? = null,
) = Country(
    id = id,
    name = name,
    aliases = aliases,
    iso2 = iso2,
    iso3 = iso3,
    isoNumeric = isoNumeric,
    phoneCode = phoneCode,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    regionId = regionId,
    subregionId = subregionId,
    nativeName = nativeName,
    tld = tld,
    flagId = flagId,
    region = region,
    subregion = subregion,
    flag = flag,
    currencies = currencies,
    languages = languages,
    timezones = timezones,
    centralBanks = centralBanks,
    states = states,
    truncatedIncludes = truncatedIncludes,
)

/**
 * Builds REST countries, resolving the relationships a caller asked to embed.
 *
 * Embedded records never carry their own relationships: that is what keeps `include` one level deep
 * and the response size predictable.
 */
@Component
class CountryRestAssembler(
    private val regionStore: RegionStore,
    private val subregionStore: SubregionStore,
    private val flagStore: FlagStore,
    private val currencyStore: CurrencyStore,
    private val languageStore: LanguageStore,
    private val timezoneStore: TimezoneStore,
    private val centralBankStore: CentralBankStore,
    private val stateStore: StateStore,
) {
    /**
     * Assembles one country.
     * @param data Country read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST country with the requested relationships embedded.
     */
    fun assemble(data: CountryData, includes: IncludeSpec, languageCode: String?): Country {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        // Aliases are the country's own data rather than a relationship, so there is nothing to load.
        val aliases = if (ALIASES in includes) data.aliases else null

        val region = assembler.one(REGION, { regionStore.byId(data.regionId, languageCode) }) { it.toRest() }
        val subregion =
            assembler.one(SUBREGION, { subregionStore.byId(data.subregionId, languageCode) }) { it.toRest() }
        val flag = assembler.one(FLAG, { data.flagId?.let(flagStore::byId) }) { it.toRest() }
        val currencies =
            assembler.many(CURRENCIES, { currencyStore.byCountryId(data.id, languageCode) }) { it.toRest() }
        val languages =
            assembler.many(LANGUAGES, { languageStore.byCountryId(data.id, languageCode) }) { it.toRest() }
        val timezones =
            assembler.many(TIMEZONES, { timezoneStore.byCountryId(data.id, languageCode) }) { it.toRest() }
        val centralBanks =
            assembler.many(
                CENTRAL_BANKS,
                { centralBankStore.byCountryId(data.id, languageCode) },
            ) { it.toRest() }
        val states = assembler.manyPaged(STATES, { statesOf(data.id, languageCode) }) { it.toRest() }

        return data.toRest(
            aliases = aliases,
            region = region,
            subregion = subregion,
            flag = flag,
            currencies = currencies,
            languages = languages,
            timezones = timezones,
            centralBanks = centralBanks,
            states = states,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    /**
     * Reads the first page of a country's states.
     *
     * The page carries the true total, so truncation is detected without materializing every state
     * of a large country just to throw most of them away.
     */
    private fun statesOf(countryId: String, languageCode: String?): Page<StateData> = stateStore
        .pageByCountryId(
            countryId = countryId,
            request = PageRequest(page = 0, size = IncludeSpec.MAX_INCLUDED_ITEMS),
            query = null,
            languageCode = languageCode,
        )

    companion object {
        const val REGION = "region"
        const val SUBREGION = "subregion"
        const val FLAG = "flag"
        const val CURRENCIES = "currencies"
        const val LANGUAGES = "languages"
        const val TIMEZONES = "timezones"
        const val CENTRAL_BANKS = "centralBanks"
        const val STATES = "states"
        const val ALIASES = "aliases"

        /**
         * Relationships a country endpoint accepts, plus `aliases`, which adds a field rather than a
         * record: every country has a handful of them, too many to send to callers who never read them.
         *
         * Cities are deliberately absent: a single country can have tens of thousands, so they are
         * only reachable through their own paginated endpoint.
         */
        val ALLOWED_INCLUDES = setOf(
            REGION, SUBREGION, FLAG, CURRENCIES, LANGUAGES, TIMEZONES, CENTRAL_BANKS, STATES, ALIASES,
        )
    }
}
