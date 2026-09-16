package dev.voir.sole.world.api.currency

import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.centralbank.toRest
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toRest
import dev.voir.sole.world.api.model.CurrencyData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.CentralBank
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Currency
import dev.voir.sole.world.openapi.model.Flag
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Component

/**
 * Maps a currency onto its published REST type.
 * @return REST currency with any supplied relationships embedded.
 */
fun CurrencyData.toRest(
    flag: Flag? = null,
    replacedBy: Currency? = null,
    countries: List<Country>? = null,
    centralBanks: List<CentralBank>? = null,
    truncatedIncludes: List<String>? = null,
) = Currency(
    id = id,
    iso3 = iso3,
    isoNumeric = isoNumeric,
    name = name,
    decimalDigits = decimalDigits,
    obsolete = obsolete,
    nativeName = nativeName,
    description = description,
    symbol = symbol,
    year = year,
    introducedDate = introducedDate?.toJavaLocalDate(),
    obsoleteAt = obsoleteAt?.toJavaLocalDate(),
    replacedById = replacedById,
    flagId = flagId,
    flag = flag,
    replacedBy = replacedBy,
    countries = countries,
    centralBanks = centralBanks,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST currencies, resolving the relationships a caller asked to embed. */
@Component
class CurrencyRestAssembler(
    private val currencyStore: CurrencyStore,
    private val countryStore: CountryStore,
    private val centralBankStore: CentralBankStore,
    private val flagStore: FlagStore,
) {
    /**
     * Assembles one currency.
     * @param data Currency read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST currency with the requested relationships embedded.
     */
    fun assemble(data: CurrencyData, includes: IncludeSpec, languageCode: String?): Currency {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val flag = assembler.one(FLAG, { data.flagId?.let(flagStore::byId) }) { it.toRest() }
        val replacedBy = assembler.one(
            REPLACED_BY,
            { data.replacedById?.let { currencyStore.byId(it, languageCode) } },
        ) { it.toRest() }
        val countries =
            assembler.many(COUNTRIES, { countryStore.byCurrencyId(data.id, languageCode) }) { it.toRest() }
        val centralBanks =
            assembler.many(
                CENTRAL_BANKS,
                { centralBankStore.byCurrencyId(data.id, languageCode) },
            ) { it.toRest() }

        return data.toRest(
            flag = flag,
            replacedBy = replacedBy,
            countries = countries,
            centralBanks = centralBanks,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val FLAG = "flag"
        const val REPLACED_BY = "replacedBy"
        const val COUNTRIES = "countries"
        const val CENTRAL_BANKS = "centralBanks"

        /** Relationships a currency endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(FLAG, REPLACED_BY, COUNTRIES, CENTRAL_BANKS)
    }
}
