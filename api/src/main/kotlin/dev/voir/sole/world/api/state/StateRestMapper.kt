package dev.voir.sole.world.api.state

import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.city.toRest
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.model.StateData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.City
import dev.voir.sole.world.openapi.model.Coordinates
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.State
import org.springframework.stereotype.Component

/**
 * Maps a state onto its published REST type.
 * @return REST state with any supplied relationships embedded.
 */
fun StateData.toRest(
    country: Country? = null,
    cities: List<City>? = null,
    truncatedIncludes: List<String>? = null,
) = State(
    id = id,
    name = name,
    countryId = countryId,
    stateCode = stateCode,
    type = type,
    coordinates = if (latitude == null || longitude == null) {
        null
    } else {
        Coordinates(latitude = latitude, longitude = longitude)
    },
    country = country,
    cities = cities,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST states, resolving the relationships a caller asked to embed. */
@Component
class StateRestAssembler(
    private val countryStore: CountryStore,
    private val cityStore: CityStore,
) {
    /**
     * Assembles one state.
     * @param data State read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST state with the requested relationships embedded.
     */
    fun assemble(data: StateData, includes: IncludeSpec, languageCode: String?): State {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        val country = assembler.one(
            COUNTRY,
            { countryStore.byId(data.countryId, languageCode) },
        ) { it.toRest() }
        val cities = assembler.manyPaged(
            CITIES,
            {
                cityStore.pageByStateId(
                    stateId = data.id,
                    request = PageRequest(page = 0, size = IncludeSpec.MAX_INCLUDED_ITEMS),
                    query = null,
                    languageCode = languageCode,
                )
            },
        ) { it.toRest() }

        return data.toRest(
            country = country,
            cities = cities,
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val COUNTRY = "country"
        const val CITIES = "cities"

        /** Relationships a state endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(COUNTRY, CITIES)
    }
}
