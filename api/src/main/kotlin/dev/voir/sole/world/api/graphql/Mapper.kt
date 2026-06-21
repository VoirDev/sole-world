package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.model.*
import dev.voir.sole.world.graphql.dto.types.*

/**
 * Maps CountryData to its generated GraphQL DTO.
 * @return GraphQL country DTO.
 */
fun CountryData.toGql() = Country(
    id = this.id.toString(),
    name = this.name,
    nativeName = this.nativeName,
    iso3 = this.iso3,
    iso2 = this.iso2,
    isoNumeric = this.isoNumeric,
    phoneCode = this.phoneCode,
    tld = this.tld,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    regionId = this.regionId.toString(),
    subregionId = this.subregionId.toString(),
    flagId = this.flagId?.toString(),
    states = emptyStatePage(),
    cities = emptyCityPage(),
)

private fun emptyStatePage() = StatePage(
    items = emptyList(),
    pageInfo = emptyPageInfo(),
)

private fun emptyCityPage() = CityPage(
    items = emptyList(),
    pageInfo = emptyPageInfo(),
)

private fun emptyPageInfo() = PageInfo(
    page = 0,
    size = 0,
    totalItems = 0,
    totalPages = 0,
    hasNextPage = false,
    hasPreviousPage = false,
)

/**
 * Maps CurrencyData to its generated GraphQL DTO.
 * @return GraphQL currency DTO.
 */
fun CurrencyData.toGql() = Currency(
    id = this.id.toString(),
    iso3 = this.iso3,
    isoNumeric = this.isoNumeric,
    name = this.name,
    decimalDigits = this.decimalDigits,
    description = this.description,
    nativeName = this.nativeName,
    symbol = this.symbol,
    year = this.year,
    introducedDate = this.introducedDate,
    obsolete = this.obsolete,
    obsoleteAt = this.obsoleteAt,
    replacedById = this.replacedById?.toString(),
    flagId = this.flagId?.toString(),
)

/**
 * Maps LanguageData to its generated GraphQL DTO.
 * @return GraphQL language DTO.
 */
fun LanguageData.toGql() = Language(
    id = this.id.toString(),
    code = this.code,
    nativeName = this.nativeName,
    name = this.name,
    description = this.description,
    flagId = this.flagId?.toString(),
)

/**
 * Maps FlagData to its generated GraphQL DTO.
 * @return GraphQL flag DTO.
 */
fun FlagData.toGql() = Flag(
    id = this.id.toString(),
    caption = this.caption,
    emoji = this.emoji,
    emojiU = this.emojiU,
    squareAssetId = this.squareAssetId?.toString(),
    wideAssetId = this.wideAssetId?.toString(),
)

/**
 * Maps RegionData to its generated GraphQL DTO.
 * @return GraphQL region DTO.
 */
fun RegionData.toGql() = Region(
    id = this.id.toString(),
    name = this.name
)

/**
 * Maps SubregionData to its generated GraphQL DTO.
 * @return GraphQL subregion DTO.
 */
fun SubregionData.toGql() = Subregion(
    id = this.id.toString(),
    name = this.name,
    regionId = this.regionId.toString(),
)

/**
 * Maps TimezoneData to its generated GraphQL DTO.
 * @return GraphQL timezone DTO.
 */
fun TimezoneData.toGql() = Timezone(
    id = this.id.toString(),
    zoneName = this.zoneName,
    tzName = this.tzName,
    gmtOffset = this.gmtOffset,
    gmtOffsetName = this.gmtOffsetName,
    abbreviation = this.abbreviation,
)

/**
 * Maps CentralBankData to its generated GraphQL DTO.
 * @return GraphQL central bank DTO.
 */
fun CentralBankData.toGql(): CentralBank = CentralBank(
    id = this.id.toString(),
    name = this.name,
    nativeName = this.nativeName,
    websiteUrl = this.websiteURL,
    establishmentYear = this.establishmentYear,
)

/**
 * Maps StateData to its generated GraphQL DTO.
 * @return GraphQL state or province DTO.
 */
fun StateData.toGql() = State(
    id = this.id.toString(),
    name = this.name,
    stateCode = this.stateCode,
    coordinates = if (this.latitude == null || this.longitude == null) null else
        Coordinates(latitude = this.latitude, longitude = this.longitude),
    type = this.type,
    countryId = this.countryId.toString()
)

/**
 * Maps CityData to its generated GraphQL DTO.
 * @return GraphQL city DTO.
 */
fun CityData.toGql() = City(
    id = this.id.toString(),
    name = this.name,
    coordinates = Coordinates(latitude = this.latitude, longitude = this.longitude)
)

/**
 * Maps MediaAssetData to its generated GraphQL DTO.
 * @return GraphQL media asset DTO.
 */
fun MediaAssetData.toGql() = MediaAsset(
    id = this.id.toString(),
    type = when (this.type) {
        MediaAssetTypeData.Image -> MediaAssetType.Image
    },
    image = if (this.type == MediaAssetTypeData.Image) {
        ImageAsset(
            aspectRatio = when (this.imageAspectRatio) {
                MediaAssetImageAspectRatioData.Wide -> ImageAssetAspectRatio.Wide
                MediaAssetImageAspectRatioData.Square -> ImageAssetAspectRatio.Square
                else -> throw Exception("Image aspect ratio not resolved")
            },
            formats = this.imageFormats!!.toGql()
        )
    } else null
)

/**
 * Maps MediaAssetImageFormatsData to its generated GraphQL DTO.
 * @return GraphQL image formats DTO.
 */
fun MediaAssetImageFormatsData.toGql() = ImageAssetFormats(
    svg = this.svg,
    png = this.png?.toGql(),
    webp = this.webp?.toGql(),
    jpg = this.jpg?.toGql(),
)

/**
 * Maps MediaAssetImageSizesData to its generated GraphQL DTO.
 * @return GraphQL image sizes DTO.
 */
fun MediaAssetImageSizesData.toGql() = ImageAssetSizes(
    xs = this.xs,
    sm = this.sm,
    md = this.md,
    lg = this.lg,
    xl = this.xl,
)
