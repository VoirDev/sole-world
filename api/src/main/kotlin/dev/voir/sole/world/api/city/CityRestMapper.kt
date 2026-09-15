package dev.voir.sole.world.api.city

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toRest
import dev.voir.sole.world.api.model.CityData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.state.toRest
import dev.voir.sole.world.openapi.model.City
import dev.voir.sole.world.openapi.model.Coordinates
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.State
import org.springframework.stereotype.Component

/**
 * Maps a city onto its published REST type.
 * @return REST city with any supplied relationships embedded.
 */
fun CityData.toRest(
    state: State? = null,
    country: Country? = null,
    truncatedIncludes: List<String>? = null,
) = City(
    id = id,
    name = name,
    stateId = stateId,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    state = state,
    country = country,
    truncatedIncludes = truncatedIncludes,
)

/** Builds REST cities, resolving the relationships a caller asked to embed. */
@Component
class CityRestAssembler(
    private val stateStore: StateStore,
    private val countryStore: CountryStore,
) {
    /**
     * Assembles one city.
     * @param data City read from the store.
     * @param includes Relationships the caller asked for.
     * @param languageCode Language the response is rendered in.
     * @return REST city with the requested relationships embedded.
     */
    fun assemble(data: CityData, includes: IncludeSpec, languageCode: String?): City {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)
        val state = stateStore.byId(data.stateId, languageCode)

        return data.toRest(
            state = assembler.one(STATE, { state }) { it.toRest() },
            // A city's country is reached through its state, which the dataset always provides.
            country = assembler.one(
                COUNTRY,
                { state?.let { countryStore.byId(it.countryId, languageCode) } },
            ) { it.toRest() },
            truncatedIncludes = assembler.truncatedIncludes(),
        )
    }

    companion object {
        const val STATE = "state"
        const val COUNTRY = "country"

        /** Relationships a city endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(STATE, COUNTRY)
    }
}
