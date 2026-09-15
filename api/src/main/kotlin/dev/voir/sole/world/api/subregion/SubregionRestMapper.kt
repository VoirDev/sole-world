package dev.voir.sole.world.api.subregion

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.model.SubregionData
import dev.voir.sole.world.api.region.RegionStore
import dev.voir.sole.world.api.region.toRest
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Region
import dev.voir.sole.world.openapi.model.Subregion
import org.springframework.stereotype.Component

/**
 * Maps a subregion onto its published REST type.
 * @return REST subregion with any supplied relationships embedded.
 */
fun SubregionData.toRest(
    region: Region? = null,
    countries: List<Country>? = null,
    truncatedIncludes: List<String>? = null,
) = Subregion(
    id = id,
    name = name,
    regionId = regionId,
    region = region,
    countries = countries,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST subregions, resolving the relationships a caller asked to embed. */
@Component
class SubregionRestAssembler(
    private val regionStore: RegionStore,
    private val countryStore: CountryStore,
) {
    /**
     * Assembles one subregion.
     * @param data Subregion read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST subregion with the requested relationships embedded.
     */
    fun assemble(data: SubregionData, includes: IncludeSpec, languageCode: String?): Subregion {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val region = assembler.one(REGION, { regionStore.byId(data.regionId, languageCode) }) { it.toRest() }
        val countries =
            assembler.many(COUNTRIES, { countryStore.bySubregionId(data.id, languageCode) }) { it.toRest() }

        return data.toRest(
            region = region,
            countries = countries,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val REGION = "region"
        const val COUNTRIES = "countries"

        /** Relationships a subregion endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(REGION, COUNTRIES)
    }
}
