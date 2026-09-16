package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.mediaasset.MediaAssetStore
import dev.voir.sole.world.api.mediaasset.toRest
import dev.voir.sole.world.api.model.CryptoData
import dev.voir.sole.world.api.rest.IncludeAssembler
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.openapi.model.Crypto
import dev.voir.sole.world.openapi.model.MediaAsset
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Component

/**
 * Maps a cryptocurrency onto its published REST type.
 * @return REST cryptocurrency.
 */
fun CryptoData.toRest(logo: MediaAsset? = null) = Crypto(
    id = id,
    code = code,
    alias = alias,
    name = name,
    decimalDigits = decimalDigits,
    obsolete = obsolete,
    description = description,
    websiteUrl = websiteUrl,
    introducedYear = introducedYear,
    obsoleteAt = obsoleteAt?.toJavaLocalDate(),
    logoId = logoId,
    logo = logo,
)

/** Builds REST cryptocurrencies, resolving the logo a caller asked to embed. */
@Component
class CryptoRestAssembler(
    private val mediaAssetStore: MediaAssetStore,
) {
    /**
     * Assembles one cryptocurrency.
     * @param data Cryptocurrency read from the store.
     * @param includes Relationships the caller asked for.
     * @return REST cryptocurrency with the requested relationships embedded.
     */
    fun assemble(data: CryptoData, includes: IncludeSpec): Crypto {
        if (includes.isEmpty()) {
            return data.toRest()
        }

        val assembler = IncludeAssembler(includes)

        return data.toRest(
            logo = assembler.one(LOGO, { data.logoId?.let(mediaAssetStore::byId) }, { it.toRest() }),
        )
    }

    companion object {
        const val LOGO = "logo"

        /** Relationships a cryptocurrency endpoint accepts. */
        val ALLOWED_INCLUDES = setOf(LOGO)
    }
}
