package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.CryptosApi
import dev.voir.sole.world.openapi.model.Crypto
import dev.voir.sole.world.openapi.model.CryptoPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for cryptocurrencies. */
@RestController
class CryptoRestController(
    private val cryptoStore: CryptoStore,
    private val cryptos: CryptoRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : CryptosApi {
    override fun listCryptos(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        obsolete: Boolean?,
    ): ResponseEntity<CryptoPage> {
        val includes = IncludeSpec.parse(include, CryptoRestAssembler.ALLOWED_INCLUDES)

        val result = cryptoStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = RestRequest.language(request),
            query = RestRequest.searchQuery(query),
            obsolete = obsolete,
        )

        return cache.ok(
            CryptoPage(
                items = result.items.map { cryptos.assemble(it, includes) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getCrypto(id: String, lang: String?, include: String?): ResponseEntity<Crypto> {
        val includes = IncludeSpec.parse(include, CryptoRestAssembler.ALLOWED_INCLUDES)
        val crypto = cryptoStore.resolve(id)
            ?: throw ResourceNotFoundException.of("Cryptocurrency", id)

        return cache.ok(cryptos.assemble(crypto, includes), request)
    }
}
