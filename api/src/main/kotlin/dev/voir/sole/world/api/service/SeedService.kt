package dev.voir.sole.world.api.service

import dev.voir.sole.world.api.database.CentralBankRepository
import dev.voir.sole.world.api.database.CityRepository
import dev.voir.sole.world.api.database.CountryRepository
import dev.voir.sole.world.api.database.StateRepository
import dev.voir.sole.world.api.database.table.*
import dev.voir.sole.world.api.model.*
import dev.voir.sole.world.api.service.ImageSizeJSON.Companion.toData
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service

@Service
class SeedService(
    private val countryRepository: CountryRepository,
    private val stateRepository: StateRepository,
    private val cityRepository: CityRepository,
    private val centralBankRepository: CentralBankRepository,
) {
    private val logger = LoggerFactory.getLogger(SeedService::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** Imports bundled seed data when the bundled version differs from the stored database version. */
    fun import() {
        val newVersion = bundledDataVersion()

        val currentVersion = currentDataVersion()

        // Keep startup cheap when the bundled data version already matches the database.
        if (!shouldImportData(currentVersion, newVersion)) {
            this.logger.info("Data version unchanged ($newVersion), skipping database entity update")
            return
        }

        this.logger.info("Data version changed: $currentVersion -> $newVersion")

        clearAll()

        try {
            importMediaAssets()
            importFlags()
            importLanguages()
            importCurrencies()
            importTimezones()
            importRegions()
            importCountries()
            importCentralBanks()

            updateDataVersion(newVersion)

            this.logger.info("All data updated and version changed!")
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Reads the bundled seed data version from meta.json.
     * @return Version number packaged with the application.
     */
    fun bundledDataVersion(): Int {
        return readJSONFromFile<MetaJSON>("data/meta.json").version
    }

    /**
     * Reads the currently imported data version from the database.
     * @return Stored data version, or null when the database has not been seeded.
     */
    fun currentDataVersion() = transaction {
        return@transaction DataImportVersion.selectAll()
            .where { DataImportVersion.id eq 1 }
            .map { it[DataImportVersion.version] }
            .firstOrNull()
    }

    /**
     * Decides whether bundled seed data should be applied to database entities.
     * @param currentVersion Version currently recorded in the database.
     * @param bundledVersion Version packaged in data/meta.json.
     * @return True only when the database is missing a version or the bundled version changed.
     */
    fun shouldImportData(currentVersion: Int?, bundledVersion: Int): Boolean {
        return currentVersion != bundledVersion
    }

    /**
     * Stores the seed data version after a successful import.
     * @param newVersion Version imported from data/meta.json.
     */
    private fun updateDataVersion(newVersion: Int) = transaction {
        val updatedRows = DataImportVersion.update({ DataImportVersion.id eq 1 }) {
            it[version] = newVersion
        }

        if (updatedRows == 0) {
            DataImportVersion.insert {
                it[id] = 1
                it[version] = newVersion
            }
        }
    }

    /** Imports media asset metadata before records that reference those assets. */
    private fun importMediaAssets() {
        val mediaAssets = readJSONFromFile<List<MediaAssetJSON>>("data/media_assets.json")
        this.logger.info("Importing ${mediaAssets.count()} media assets...")

        transaction {
            // Insert only missing media records so reruns remain idempotent inside one import.
            for (mediaAsset in mediaAssets) {
                MediaAssetEntity.findById(mediaAsset.id)
                    ?: MediaAssetEntity.new(mediaAsset.id) {
                        type = mediaAsset.type
                        imageAspectRatio = mediaAsset.imageAspectRatio
                        imageFormats = MediaAssetImageFormatsData(
                            svg = mediaAsset.imageFormats.svg,
                            png = mediaAsset.imageFormats.png?.toData(),
                            jpg = mediaAsset.imageFormats.jpg?.toData(),
                            webp = mediaAsset.imageFormats.webp?.toData()
                        )
                        description = mediaAsset.description
                    }
            }
        }
    }

    /** Imports shared flag metadata before records that reference flags. */
    private fun importFlags() {
        val flags = readJSONFromFile<List<FlagJSON>>("data/flags.json")
        this.logger.info("Importing ${flags.count()} flags...")

        transaction {
            for (flag in flags) {
                FlagEntity.findById(flag.id)
                    ?: FlagEntity.new(flag.id) {
                        caption = flag.caption
                        emoji = flag.emoji
                        emojiU = flag.emojiU
                        squareAssetId = flag.square?.let { EntityID(it, MediaAssetsTable) }
                        wideAssetId = flag.wide?.let { EntityID(it, MediaAssetsTable) }
                    }
            }
        }
    }

    /** Imports regions, nested subregions, and their translations. */
    private fun importRegions() {
        val regions = readJSONFromFile<List<RegionJSON>>("data/regions.json")
        this.logger.info("Importing ${regions.count()} regions...")
        transaction {
            // Regions own their nested subregions, so both levels are imported in one transaction.
            for (region in regions) {
                val regionEntity =
                    RegionEntity.findById(region.id) ?: RegionEntity.new(region.id) {
                        name = region.name
                        wikiDataId = region.wikiDataId
                    }

                RegionTranslationsTable.batchInsert(region.translations) { regionTranslationJSON ->
                    this[RegionTranslationsTable.region] = regionEntity.id
                    this[RegionTranslationsTable.languageCode] = regionTranslationJSON.languageCode
                    this[RegionTranslationsTable.name] = regionTranslationJSON.name
                }

                for (subregion in region.subregions) {
                    val subregionEntity =
                        SubregionEntity.findById(subregion.id)
                            ?: SubregionEntity.new(subregion.id) {
                                name = subregion.name
                                wikiDataId = subregion.wikiDataId
                                this.region = regionEntity
                            }

                    SubregionTranslationsTable.batchInsert(subregion.translations) { subregionTranslationJSON ->
                        this[SubregionTranslationsTable.subregion] = subregionEntity.id
                        this[SubregionTranslationsTable.languageCode] =
                            subregionTranslationJSON.languageCode
                        this[SubregionTranslationsTable.name] = subregionTranslationJSON.name
                    }
                }
            }
        }
    }

    /** Imports languages and localized language names. */
    private fun importLanguages() {
        val languages = readJSONFromFile<List<LanguageJSON>>("data/languages.json")
        this.logger.info("Importing ${languages.count()} languages...")

        transaction {
            for (language in languages) {
                val entity =
                    LanguageEntity.findById(language.id) ?: LanguageEntity.new(language.id) {
                        name = language.name
                        code = language.code
                        nativeName = language.nativeName
                        flagId = language.flagId?.let { EntityID(it, FlagsTable) }
                    }

                LanguageTranslationsTable.batchInsert(language.translations) { translationJSON ->
                    this[LanguageTranslationsTable.language] = entity.id
                    this[LanguageTranslationsTable.languageCode] = translationJSON.languageCode
                    this[LanguageTranslationsTable.name] = translationJSON.name
                }
            }
        }
    }

    /** Imports currencies and localized currency names. */
    private fun importCurrencies() {
        val currencies = readJSONFromFile<List<CurrencyJSON>>("data/currencies.json")
        this.logger.info("Importing ${currencies.count()} currencies...")

        transaction {
            for (currency in currencies) {
                val entity =
                    CurrencyEntity.findById(currency.id) ?: CurrencyEntity.new(currency.id) {
                        iso3 = currency.iso3
                        isoNumeric = currency.isoNumeric
                        name = currency.name
                        description = currency.description
                        nativeName = currency.nativeName
                        symbol = currency.symbol
                        year = currency.year
                        introducedDate =
                            null // TODO Currency introduced date not provided right now
                        obsolete = currency.obsolete
                        obsoleteAt = null // TODO Currency obsolete date not provided right now
                        replacedBy = currency.replacedBy?.let {
                            EntityID(currency.replacedBy, CurrenciesTable)
                        }
                        this.flagId = currency.flagId?.let { EntityID(it, FlagsTable) }
                        decimalDigits = currency.decimalDigits

                    }

                CurrenciesTranslationsTable.batchInsert(currency.translations) { translationJSON ->
                    this[CurrenciesTranslationsTable.currency] = entity.id
                    this[CurrenciesTranslationsTable.languageCode] =
                        translationJSON.languageCode
                    this[CurrenciesTranslationsTable.name] = translationJSON.name
                }
            }
        }
    }

    /** Imports timezones and localized timezone labels. */
    private fun importTimezones() {
        val timezones = readJSONFromFile<List<TimezoneJSON>>("data/timezones.json")
        this.logger.info("Importing ${timezones.count()} timezones...")

        transaction {
            for (timezone in timezones) {
                val entity =
                    TimezoneEntity.findById(timezone.id) ?: TimezoneEntity.new(timezone.id) {
                        tzName = timezone.tzName
                        zoneName = timezone.zoneName
                        abbreviation = timezone.abbreviation
                        gmtOffset = timezone.gmtOffset
                        gmtOffsetName = timezone.gmtOffsetName
                    }
                TimezoneTranslationsTable.batchInsert(timezone.translations) { translationJSON ->
                    this[TimezoneTranslationsTable.timezone] = entity.id
                    this[TimezoneTranslationsTable.languageCode] = translationJSON.languageCode
                    this[TimezoneTranslationsTable.tzName] = translationJSON.tzName
                }
            }
        }
    }

    /** Imports country files, including country translations, relationships, states, cities, and nested translations. */
    private fun importCountries() {
        val countryFolders = listFoldersOnPath("data/countries")

        this.logger.info("Importing ${countryFolders.count()} countries...")

        // Each country has its own resource folder with states, cities, and relationship ids.
        countryFolders.forEach { folder ->
            val countryJSON = readJSONFromFile<CountryJSON>("data/countries/$folder/data.json")

            countryRepository.createCountries(
                listOf(
                    NewCountryData(
                        id = countryJSON.id,
                        name = countryJSON.name,
                        nativeName = countryJSON.nativeName,
                        iso2 = countryJSON.iso2,
                        iso3 = countryJSON.iso3,
                        isoNumeric = countryJSON.numericCode,
                        phoneCode = countryJSON.phoneCode,
                        tld = countryJSON.tld,
                        latitude = countryJSON.latitude,
                        longitude = countryJSON.longitude,
                        flagId = countryJSON.flagId,
                        regionId = countryJSON.regionId,
                        subregionId = countryJSON.subregionId,
                    )
                )
            )

            countryRepository.createCountryTranslations(countryJSON.translations.map {
                NewCountryTranslationData(
                    languageCode = it.languageCode,
                    name = it.name,
                    countryId = countryJSON.id
                )
            })

            countryRepository.setCountryCurrencies(
                countryId = countryJSON.id,
                currencyIds = countryJSON.currencyIds
            )

            countryRepository.setCountryTimezones(
                countryId = countryJSON.id,
                timezoneIds = countryJSON.timezoneIds
            )

            countryRepository.setCountryLanguages(
                countryId = countryJSON.id,
                languageIds = mapOf(
                    CountryLanguageType.Official to countryJSON.officialLanguageIds,
                    CountryLanguageType.Other to countryJSON.otherLanguageIds
                )
            )

            stateRepository.createStates(countryJSON.states.map {
                NewStateData(
                    id = it.id,
                    name = it.name,
                    stateCode = it.stateCode,
                    type = it.type,
                    longitude = it.longitude,
                    latitude = it.latitude,
                    countryId = countryJSON.id
                )
            })

            stateRepository.createStateTranslations(countryJSON.states.flatMap { state ->
                state.translations.map { translation ->
                    NewStateTranslationData(
                        languageCode = translation.languageCode,
                        name = translation.name,
                        stateId = state.id
                    )
                }
            })

            cityRepository.createCities(countryJSON.states.flatMap { state ->
                state.cities.map { city ->
                    NewCityData(
                        id = city.id,
                        name = city.name,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        stateId = state.id
                    )
                }
            })
            cityRepository.createCityTranslations(countryJSON.states.flatMap { state ->
                state.cities.flatMap { city ->
                    city.translations.map { translation ->
                        NewCityTranslation(
                            languageCode = translation.languageCode,
                            name = translation.name,
                            cityId = city.id
                        )
                    }
                }
            })

        }
    }

    /** Imports central banks, translations, served countries, and issued currencies. */
    private fun importCentralBanks() {
        val centralBanks = readJSONFromFile<List<CentralBankJSON>>("data/central_banks.json")
        this.logger.info("Importing ${centralBanks.count()} central banks...")

        transaction {
            for (centralBank in centralBanks) {
                centralBankRepository.createCentralBanks(
                    listOf(
                        NewCentralBankData(
                            id = centralBank.id,
                            name = centralBank.name,
                            nativeName = centralBank.nativeName,
                            websiteURL = centralBank.websiteURL,
                            establishmentYear = centralBank.establishmentYear,
                        )
                    )
                )

                CentralBankTranslationsTable.batchInsert(centralBank.translations) { translationJSON ->
                    this[CentralBankTranslationsTable.centralBank] = centralBank.id
                    this[CentralBankTranslationsTable.languageCode] = translationJSON.languageCode
                    this[CentralBankTranslationsTable.name] = translationJSON.name
                }

                centralBankRepository.setCentralBankCountries(
                    centralBankId = centralBank.id,
                    countryIds = centralBank.countryIds,
                )

                centralBankRepository.setCentralBankCurrencies(
                    centralBankId = centralBank.id,
                    currencyIds = centralBank.currencyIds
                )
            }
        }
    }

    /** Deletes all imported data in dependency order before reseeding. */
    private fun clearAll() {
        this.logger.info("Clearing all data...")
        transaction {
            // Delete child/link tables before parent tables to satisfy foreign-key constraints.
            CentralBankIssuedCurrenciesTable.deleteAll()
            CentralBankCountriesTable.deleteAll()
            CentralBankTranslationsTable.deleteAll()
            CentralBanksTable.deleteAll()

            CityTranslationsTable.deleteAll()
            CitiesTable.deleteAll()

            StateTranslationsTable.deleteAll()
            StatesTable.deleteAll()

            CountryTranslationsTable.deleteAll()
            CountryTimezonesTable.deleteAll()
            CountryCurrenciesTable.deleteAll()
            CountryLanguagesTable.deleteAll()
            CountriesTable.deleteAll()

            LanguageTranslationsTable.deleteAll()
            LanguagesTable.deleteAll()

            CurrenciesTranslationsTable.deleteAll()
            CurrenciesTable.deleteAll()

            FlagsTable.deleteAll()

            TimezoneTranslationsTable.deleteAll()
            TimezonesTable.deleteAll()

            SubregionTranslationsTable.deleteAll()
            SubregionsTable.deleteAll()

            RegionTranslationsTable.deleteAll()
            RegionsTable.deleteAll()

            MediaAssetsTable.deleteAll()
        }
    }

    /**
     * Reads a classpath JSON resource and decodes it into the requested type.
     * @param path Classpath-relative JSON resource path.
     * @return Decoded JSON payload.
     */
    private inline fun <reified T> readJSONFromFile(path: String): T {
        // ClassPathResource works both from exploded classes and from a packaged application jar.
        val resource = ClassPathResource(path)
        val text = resource.inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString<T>(text)
    }

    /**
     * Lists immediate child folder names under a classpath resource path.
     * @param path Classpath-relative folder path.
     * @return Folder names discovered under the resource path.
     */
    private fun listFoldersOnPath(path: String): List<String> {
        val resolver = PathMatchingResourcePatternResolver()
        val marker = "/$path/"

        // Match files instead of walking directories so this works from both exploded classes and packaged jars.
        return resolver.getResources("classpath*:/$path/*/data.json")
            .mapNotNull { resource ->
                resource.url.toString()
                    .substringAfter(marker, missingDelimiterValue = "")
                    .substringBefore("/data.json")
                    .takeIf { it.isNotBlank() && !it.contains("/") }
            }
            .distinct()
            .sorted()
    }
}
