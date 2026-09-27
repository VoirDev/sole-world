package dev.voir.sole.world.api.country

import dev.voir.sole.world.api.graphql.EmptyPage
import dev.voir.sole.world.api.model.CountryData
import dev.voir.sole.world.graphql.dto.types.Coordinates
import dev.voir.sole.world.graphql.dto.types.Country

/**
 * Maps a country onto its generated GraphQL type.
 * @return GraphQL country type.
 */
fun CountryData.toGql() = Country(
    id = id,
    name = name,
    nativeName = nativeName,
    iso3 = iso3,
    iso2 = iso2,
    isoNumeric = isoNumeric,
    phoneCode = phoneCode,
    tld = tld,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    regionId = regionId,
    subregionId = subregionId,
    flagId = flagId,
    states = EmptyPage.STATES,
    cities = EmptyPage.CITIES,
)
