package dev.voir.sole.world.api.crypto

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.mediaasset.MediaAssetStore
import dev.voir.sole.world.api.mediaasset.toGql
import dev.voir.sole.world.graphql.dto.types.Crypto
import dev.voir.sole.world.graphql.dto.types.CryptoPage
import dev.voir.sole.world.graphql.dto.types.MediaAsset
import dev.voir.sole.world.graphql.dto.types.PageInput

/** GraphQL fetchers for cryptocurrencies and the logos they point at. */
@DgsComponent
class CryptoGraphqlFetcher(
    private val cryptoStore: CryptoStore,
    private val mediaAssetStore: MediaAssetStore,
) {
    /**
     * Lists or searches cryptocurrencies.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param obsolete Restricts results to obsolete or active coins.
     * @return Paginated cryptocurrency result.
     */
    @DgsQuery
    fun cryptos(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument obsolete: Boolean?,
    ): CryptoPage {
        val result = cryptoStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = GraphqlRequest.language(),
            query = GraphqlRequest.optionalSearchQuery(query),
            obsolete = obsolete,
        )

        return CryptoPage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }

    /**
     * Loads cryptocurrencies by id.
     * @param ids Cryptocurrency ids to load; at most 50.
     * @return Matching coins.
     */
    @DgsQuery
    fun cryptosByIds(@InputArgument ids: List<String>): List<Crypto> =
        cryptoStore.byIds(GraphqlRequest.longIds(ids, "ids")).map { it.toGql() }

    /**
     * Loads one cryptocurrency by numeric identifier, ticker symbol, or alias.
     * @param idOrCode Caller-supplied identifier.
     * @return Matching coin, or null when nothing matches.
     */
    @DgsQuery
    fun crypto(@InputArgument idOrCode: String): Crypto? = cryptoStore.resolve(idOrCode)?.toGql()

    /** Resolves the logo of a cryptocurrency. */
    @DgsData(parentType = "Crypto", field = "logo")
    fun logo(dfe: DgsDataFetchingEnvironment): MediaAsset? {
        val crypto: Crypto = dfe.getSource() ?: return null
        val logoId = crypto.logoId ?: return null

        return mediaAssetStore.byId(GraphqlRequest.longId(logoId, "logoId"))?.toGql()
    }
}
