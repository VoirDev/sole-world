package dev.voir.sole.world.api.region

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.model.RegionData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.api.subregion.toRest
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.Region
import dev.voir.sole.world.openapi.model.Subregion
import org.springframework.stereotype.Component

/**
 * Maps a region onto its published REST type.
 * @return REST region with any supplied relationships embedded.
 */
fun RegionData.toRest(
    subregions: List<Subregion>? = null,
    countries: List<Country>? = null,
    truncatedIncludes: List<String>? = null,
) = Region(
    id = id,
    name = name,
    subregions = subregions,
    countries = countries,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST regions, resolving the relationships a caller asked to embed. */
@Component
class RegionRestAssembler(
    private val subregionStore: SubregionStore,
    private val countryStore: CountryStore,
) {
    /**
     * Assembles one region.
     * @param data Region read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST region with the requested relationships embedded.
     */
    fun assemble(data: RegionData, includes: IncludeSpec, languageCode: String?): Region {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val subregions =
            assembler.many(SUBREGIONS, { subregionStore.byRegionId(data.id, languageCode) }) { it.toRest() }
        val countries =
            assembler.many(COUNTRIES, { countryStore.byRegionId(data.id, languageCode) }) { it.toRest() }

        return data.toRest(
            subregions = subregions,
            countries = countries,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val SUBREGIONS = "subregions"
        const val COUNTRIES = "countries"

        /** Relationships a region endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(SUBREGIONS, COUNTRIES)
    }
}
