package dev.voir.sole.world.api.dataset

import dev.voir.sole.world.api.dataset.json.CentralBankJSON
import dev.voir.sole.world.api.dataset.json.CountryJSON
import dev.voir.sole.world.api.dataset.json.CryptoJSON
import dev.voir.sole.world.api.dataset.json.CurrencyJSON
import dev.voir.sole.world.api.dataset.json.FlagJSON
import dev.voir.sole.world.api.dataset.json.LanguageJSON
import dev.voir.sole.world.api.dataset.json.LocaleJSON
import dev.voir.sole.world.api.dataset.json.MediaAssetJSON
import dev.voir.sole.world.api.dataset.json.RegionJSON
import dev.voir.sole.world.api.dataset.json.TimezoneJSON

/**
 * The bundled dataset exactly as it was parsed, before any feature builds indexes over it.
 *
 * This is the single point where the on-disk shape is published. Each feature store reads what it
 * owns and derives its own records, so features never share an index. States and cities are nested
 * inside [countries], which is why those features read the country documents too.
 *
 * @property meta Dataset version and publication date from data/meta.json.
 * @property mediaAssets Media asset metadata.
 * @property flags Flag records referencing media assets.
 * @property regions Regions, each carrying its subregions.
 * @property timezones Timezone records.
 * @property currencies Currency records.
 * @property cryptos Cryptocurrency records.
 * @property languages Language records.
 * @property locales Translation locales every translated name is written in.
 * @property countries Country documents, each carrying its states and their cities.
 * @property centralBanks Central bank records with the countries and currencies they relate to.
 */
class RawDataset(
    val meta: DatasetMeta,
    val mediaAssets: List<MediaAssetJSON>,
    val flags: List<FlagJSON>,
    val regions: List<RegionJSON>,
    val timezones: List<TimezoneJSON>,
    val currencies: List<CurrencyJSON>,
    val cryptos: List<CryptoJSON>,
    val languages: List<LanguageJSON>,
    val locales: List<LocaleJSON>,
    val countries: List<CountryJSON>,
    val centralBanks: List<CentralBankJSON>,
)

/**
 * Version marker for the bundled dataset.
 * @property version Monotonic dataset version from data/meta.json.
 * @property date Publication timestamp recorded with that version.
 */
data class DatasetMeta(
    val version: Int,
    val date: String,
)
