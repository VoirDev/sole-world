package dev.voir.sole.world.api.centralbank

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.currency.toRest
import dev.voir.sole.world.api.model.CentralBankData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.CentralBank
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Currency
import org.springframework.stereotype.Component

/**
 * Maps a central bank onto its published REST type.
 * @return REST central bank with any supplied relationships embedded.
 */
fun CentralBankData.toRest(
    countries: List<Country>? = null,
    currencies: List<Currency>? = null,
    truncatedIncludes: List<String>? = null,
) = CentralBank(
    id = id,
    name = name,
    nativeName = nativeName,
    websiteUrl = websiteURL,
    establishmentYear = establishmentYear,
    countries = countries,
    currencies = currencies,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST central banks, resolving the relationships a caller asked to embed. */
@Component
class CentralBankRestAssembler(
    private val countryStore: CountryStore,
    private val currencyStore: CurrencyStore,
) {
    /**
     * Assembles one central bank.
     * @param data Central bank read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST central bank with the requested relationships embedded.
     */
    fun assemble(data: CentralBankData, includes: IncludeSpec, languageCode: String?): CentralBank {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val countries =
            assembler.many(COUNTRIES, { countryStore.byCentralBankId(data.id, languageCode) }) { it.toRest() }
        val currencies =
            assembler.many(
                CURRENCIES,
                { currencyStore.byCentralBankId(data.id, languageCode) },
            ) { it.toRest() }

        return data.toRest(
            countries = countries,
            currencies = currencies,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val COUNTRIES = "countries"
        const val CURRENCIES = "currencies"

        /** Relationships a central bank endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(COUNTRIES, CURRENCIES)
    }
}
