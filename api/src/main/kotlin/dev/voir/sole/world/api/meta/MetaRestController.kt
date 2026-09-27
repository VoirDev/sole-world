package dev.voir.sole.world.api.meta

import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.crypto.CryptoStore
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.Pagination
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.locale.LocaleStore
import dev.voir.sole.world.api.mediaasset.MediaAssetStore
import dev.voir.sole.world.api.region.RegionStore
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.api.throttle.RateLimitProperties
import dev.voir.sole.world.api.timezone.TimezoneStore
import dev.voir.sole.world.openapi.api.MetaApi
import dev.voir.sole.world.openapi.model.Meta
import dev.voir.sole.world.openapi.model.RateLimit
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * Reports what this deployment serves.
 *
 * Clients use this to discover which dataset version they are talking to, which locales they may
 * ask for, and the limits every other endpoint applies — so those limits do not have to be
 * rediscovered by triggering `400`s.
 */
@RestController
class MetaRestController(
    private val dataset: RawDataset,
    private val countryStore: CountryStore,
    private val currencyStore: CurrencyStore,
    private val cryptoStore: CryptoStore,
    private val languageStore: LanguageStore,
    private val localeStore: LocaleStore,
    private val regionStore: RegionStore,
    private val subregionStore: SubregionStore,
    private val centralBankStore: CentralBankStore,
    private val stateStore: StateStore,
    private val cityStore: CityStore,
    private val timezoneStore: TimezoneStore,
    private val flagStore: FlagStore,
    private val mediaAssetStore: MediaAssetStore,
    private val rateLimit: RateLimitProperties,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : MetaApi {
    override fun getMeta(): ResponseEntity<Meta> {
        val meta = Meta(
            datasetVersion = dataset.meta.version,
            datasetDate = dataset.meta.date,
            supportedLocales = localeStore.negotiation.supportedIds,
            maxPageSize = Pagination.MAX_PAGE_SIZE,
            maxIncludedItems = IncludeSpec.MAX_INCLUDED_ITEMS,
            counts = mapOf(
                "countries" to countryStore.size,
                "currencies" to currencyStore.size,
                "cryptos" to cryptoStore.size,
                "languages" to languageStore.size,
                "locales" to localeStore.size,
                "regions" to regionStore.size,
                "subregions" to subregionStore.size,
                "centralBanks" to centralBankStore.size,
                "states" to stateStore.size,
                "cities" to cityStore.size,
                "timezones" to timezoneStore.size,
                "flags" to flagStore.size,
                "mediaAssets" to mediaAssetStore.size,
            ),
            rateLimit = rateLimit.takeIf { it.enabled }?.let {
                RateLimit(requestsPerMinute = it.requestsPerMinute, burst = it.burst)
            },
        )

        return cache.ok(meta, request)
    }
}
