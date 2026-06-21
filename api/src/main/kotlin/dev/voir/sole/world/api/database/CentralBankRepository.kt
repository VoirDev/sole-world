package dev.voir.sole.world.api.database

import dev.voir.sole.world.api.database.table.*
import dev.voir.sole.world.api.model.CentralBankData
import dev.voir.sole.world.api.model.CentralBankData.Companion.toCentralBank
import dev.voir.sole.world.api.model.NewCentralBankData
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/** Repository for central bank persistence and lookup operations. */
@Repository
class CentralBankRepository {
    /**
     * Checks whether a central bank row exists.
     * @param id Central bank primary key.
     * @return True when a central bank exists for the supplied id.
     */
    fun hasCentralBank(id: Int): Boolean = transaction {
        CentralBanksTable
            .select(CentralBanksTable.id)
            .where { CentralBanksTable.id eq id }
            .limit(1)
            .any()
    }

    /**
     * Batch inserts central bank rows used by the seed import.
     * @param banks Central bank seed records to insert.
     * @return Exposed batch insert result.
     */
    fun createCentralBanks(banks: List<NewCentralBankData>) = transaction {
        CentralBanksTable.batchInsert(banks) { bank ->
            this[CentralBanksTable.id] = bank.id

            this[CentralBanksTable.name] = bank.name
            this[CentralBanksTable.nativeName] = bank.nativeName

            this[CentralBanksTable.websiteURL] = bank.websiteURL
            this[CentralBanksTable.establishmentYear] = bank.establishmentYear
        }
    }

    /**
     * Replaces the currencies issued by a central bank.
     * @param centralBankId Central bank primary key whose relationships should be replaced.
     * @param currencyIds Currency primary keys to associate.
     * @return Unit after the relationship collection is updated.
     */
    fun setCentralBankCurrencies(centralBankId: Int, currencyIds: List<Int>) = transaction {
        val centralBank = CentralBankEntity.findById(centralBankId)
            ?: throw Exception("Central bank with ID $centralBankId not found")

        val currencies = currencyIds.map { id ->
            val currency =
                CurrencyEntity.find { CurrenciesTable.id eq id }.firstOrNull()
                    ?: throw Exception("Currency not found: $id")
            currency
        }
        centralBank.currencies = SizedCollection(currencies)
    }

    /**
     * Replaces the countries served by a central bank.
     * @param centralBankId Central bank primary key whose relationships should be replaced.
     * @param countryIds Country primary keys to associate.
     * @return Unit after the relationship collection is updated.
     */
    fun setCentralBankCountries(centralBankId: Int, countryIds: List<Int>) = transaction {
        val centralBank = CentralBankEntity.findById(centralBankId)
            ?: throw Exception("Central bank with ID $centralBankId not found")

        val countries = countryIds.map { id ->
            val country =
                CountryEntity.find { CountriesTable.id eq id }.firstOrNull()
                    ?: throw Exception("Country not found: $id")
            country
        }
        centralBank.countries = SizedCollection(countries)
    }

    /**
     * Loads all central banks, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return All central banks mapped to API data objects.
     */
    fun getAllCentralBanks(languageCode: String?): List<CentralBankData> = transaction {
        if (languageCode == null) {
            return@transaction CentralBanksTable
                .selectAll()
                .orderBy(CentralBanksTable.name to SortOrder.ASC)
                .map { it.toCentralBank(null) }
        }

        return@transaction CentralBanksTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(CentralBankTranslationsTable.name to SortOrder.ASC)
            .distinctBy { it[CentralBanksTable.id] }
            .map { it.toCentralBank(languageCode = languageCode) }
    }

    /**
     * Counts central bank rows for paginated GraphQL listings.
     * @return Total number of central banks.
     */
    fun countCentralBanks(): Long = transaction {
        CentralBanksTable.selectAll().count()
    }

    /**
     * Loads one page of central banks, ordered by localized name when a locale is provided.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @param offset Zero-based number of rows to skip.
     * @param limit Maximum number of rows to return.
     * @return Requested central bank page mapped to API data objects.
     */
    fun getCentralBanksPage(
        languageCode: String?,
        offset: Long,
        limit: Int,
    ): List<CentralBankData> = transaction {
        if (languageCode == null) {
            return@transaction CentralBanksTable
                .selectAll()
                .orderBy(CentralBanksTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toCentralBank(null) }
        }

        return@transaction CentralBanksTable
            .joinTranslations(languageCode)
            .selectAll()
            .orderBy(CentralBankTranslationsTable.name to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
            .map { it.toCentralBank(languageCode = languageCode) }
    }

    /**
     * Loads a single central bank by primary key.
     * @param id Central bank primary key.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return The matching central bank, or null when it does not exist.
     */
    fun getCentralBankById(id: Int, languageCode: String?): CentralBankData? = transaction {
        val query = CentralBanksTable
            .joinTranslations(languageCode)
            .selectAll()
            .where { CentralBanksTable.id eq id }
            .firstOrNull() ?: return@transaction null

        query.toCentralBank(languageCode = languageCode)
    }

    /**
     * Loads central banks by primary key.
     * @param ids Primary keys to load.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Central banks matching the supplied ids.
     */
    fun getCentralBanksByIds(
        ids: List<Int>,
        languageCode: String?
    ): List<CentralBankData> = transaction {
        val centralBanks = if (languageCode == null) {
            CentralBanksTable
        } else {
            CentralBanksTable.joinTranslations(languageCode)
        }

        centralBanks
            .selectAll()
            .where { CentralBanksTable.id inList ids }
            .distinctBy { it[CentralBanksTable.id] }
            .map { it.toCentralBank(languageCode = languageCode) }
    }

    /**
     * Loads central banks grouped by country id for batched GraphQL resolution.
     * @param ids Country ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by country id with matching central banks.
     */
    fun getCentralBanksByCountryIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CentralBankData>> = transaction {
        val centralBanks = if (languageCode == null) {
            CentralBanksTable
        } else {
            CentralBanksTable.joinTranslations(languageCode)
        }

        centralBanks
            .innerJoin(CentralBankCountriesTable)
            .selectAll()
            .where { CentralBankCountriesTable.country inList ids }
            .groupBy(
                keySelector = { it[CentralBankCountriesTable.country].value },
                valueTransform = { row -> row.toCentralBank(languageCode) }
            )
            .mapValues { (_, banksForCountry) ->
                banksForCountry.distinctBy { it.id }
            }
    }

    /**
     * Loads central banks grouped by issued currency id for batched GraphQL resolution.
     * @param ids Currency ids used to group or filter related rows.
     * @param languageCode Locale code used to select translated fields; null uses base table values.
     * @return Map keyed by currency id with matching central banks.
     */
    fun getCentralBanksByCurrencyIds(
        ids: Set<Int>,
        languageCode: String?
    ): Map<Int, List<CentralBankData>> = transaction {
        val centralBanks = if (languageCode == null) {
            CentralBanksTable
        } else {
            CentralBanksTable.joinTranslations(languageCode)
        }

        centralBanks
            .innerJoin(CentralBankIssuedCurrenciesTable)
            .selectAll()
            .where { CentralBankIssuedCurrenciesTable.currency inList ids }
            .groupBy(
                keySelector = { it[CentralBankIssuedCurrenciesTable.currency].value },
                valueTransform = { row -> row.toCentralBank(languageCode) }
            )
            .mapValues { (_, banksForCurrency) ->
                banksForCurrency.distinctBy { it.id }
            }
    }
}
