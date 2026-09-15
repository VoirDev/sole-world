package dev.voir.sole.world.api.dataset

import dev.voir.sole.world.api.dataset.json.CentralBankJSON
import dev.voir.sole.world.api.dataset.json.CountryJSON
import dev.voir.sole.world.api.dataset.json.CryptoJSON
import dev.voir.sole.world.api.dataset.json.CurrencyJSON
import dev.voir.sole.world.api.dataset.json.FlagJSON
import dev.voir.sole.world.api.dataset.json.LanguageJSON
import dev.voir.sole.world.api.dataset.json.MediaAssetJSON
import dev.voir.sole.world.api.dataset.json.MetaJSON
import dev.voir.sole.world.api.dataset.json.RegionJSON
import dev.voir.sole.world.api.dataset.json.TimezoneJSON
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.time.TimeSource

/**
 * Reads the bundled dataset from the classpath into memory once, at startup.
 *
 * The dataset is immutable for the life of the image, so it is parsed into a single snapshot and
 * published as one reference. Nothing observes a half-built dataset: either the snapshot bean is
 * created and the application starts, or startup fails.
 *
 * The 200-plus country documents are the bulk of the data and are parsed in parallel; everything
 * else is a single small file.
 *
 * @property dataRoot Classpath directory the dataset is read from. Only tests change this, to load a
 * small fixture instead of the bundled data.
 */
@Configuration
class DatasetLoader(
    @Value("\${dataset.root:data}")
    private val dataRoot: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Parses the bundled dataset.
     * @return Immutable snapshot of every bundled record.
     */
    @Bean
    fun rawDataset(): RawDataset {
        val started = TimeSource.Monotonic.markNow()

        val meta = read<MetaJSON>(ClassPathResource("$dataRoot/meta.json"))
        val countries = readCountriesInParallel()

        val dataset = RawDataset(
            meta = DatasetMeta(version = meta.version, date = meta.date),
            mediaAssets = readList<MediaAssetJSON>("media_assets.json"),
            flags = readList<FlagJSON>("flags.json"),
            regions = readList<RegionJSON>("regions.json"),
            timezones = readList<TimezoneJSON>("timezones.json"),
            currencies = readList<CurrencyJSON>("currencies.json"),
            cryptos = readList<CryptoJSON>("cryptos.json"),
            languages = readList<LanguageJSON>("languages.json"),
            countries = countries,
            centralBanks = readList<CentralBankJSON>("central_banks.json"),
        )

        DatasetIntegrity.check(dataset)

        log.info(
            "Loaded dataset version {} in {}: {} countries, {} states, {} cities, {} currencies, " +
                "{} cryptocurrencies, {} media assets",
            dataset.meta.version,
            started.elapsedNow(),
            dataset.countries.size,
            dataset.countries.sumOf { it.states.size },
            dataset.countries.sumOf { country -> country.states.sumOf { it.cities.size } },
            dataset.currencies.size,
            dataset.cryptos.size,
            dataset.mediaAssets.size,
        )

        return dataset
    }

    /**
     * Parses every bundled country document concurrently.
     * @return Country documents ordered by their resource path, so the load is reproducible.
     */
    private fun readCountriesInParallel(): List<CountryJSON> {
        val resources = PathMatchingResourcePatternResolver()
            .getResources("classpath*:/$dataRoot/countries/*/data.json")
            .sortedBy { it.url.toString() }

        check(resources.isNotEmpty()) {
            "No country documents found on the classpath under $dataRoot/countries/. " +
                "The bundled dataset is missing from this build."
        }

        // Parsing is CPU-bound, so the pool is sized to the machine rather than to the file count.
        val parallelism = minOf(resources.size, Runtime.getRuntime().availableProcessors())
        val executor = Executors.newFixedThreadPool(parallelism) { runnable ->
            Thread(runnable, "dataset-loader").apply { isDaemon = true }
        }

        return try {
            executor
                .invokeAll(resources.map { resource -> Callable { read<CountryJSON>(resource) } })
                .map { it.get() }
        } finally {
            executor.shutdown()
        }
    }

    private inline fun <reified T> readList(fileName: String): List<T> {
        return read<List<T>>(ClassPathResource("$dataRoot/$fileName"))
    }

    private inline fun <reified T> read(resource: Resource): T {
        // ClassPathResource works both from exploded classes and from a packaged application jar.
        val text = resource.inputStream.bufferedReader().use { it.readText() }

        return try {
            json.decodeFromString<T>(text)
        } catch (failure: Exception) {
            throw IllegalStateException(
                "Failed to parse bundled dataset file ${resource.description}",
                failure,
            )
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(DatasetLoader::class.java)
    }
}
