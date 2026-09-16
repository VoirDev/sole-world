package dev.voir.sole.world.api.graphql

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import kotlinx.datetime.LocalDate
import java.util.Locale

/**
 * Registers the custom scalar the schema declares.
 *
 * There is exactly one. `LocalDate` carries the two dates in the dataset — when a currency was
 * introduced and when it or a coin became obsolete — and nothing else in the contract needs a scalar
 * beyond what GraphQL defines.
 */
@DgsComponent
class GraphQLScalarConfig {
    /**
     * Adds the schema's scalar to DGS runtime wiring.
     * @param builder Runtime wiring builder supplied by DGS.
     * @return Builder with the scalar registered.
     */
    @DgsRuntimeWiring
    fun addScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder.scalar(dateScalar)

    private val dateScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("LocalDate")
        .description("kotlinx.datetime.LocalDate scalar")
        .coercing(object : Coercing<LocalDate, String> {
            /**
             * Serializes the scalar backing value to its GraphQL string representation.
             * @param dataFetcherResult Value returned by a data fetcher.
             * @param graphQLContext GraphQL request context.
             * @param locale Request locale.
             * @return Serialized scalar value.
             */
            override fun serialize(
                dataFetcherResult: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): String =
                when (dataFetcherResult) {
                    is LocalDate -> dataFetcherResult.toString()
                    else -> throw CoercingSerializeException("Expected kotlinx.datetime.LocalDate")
                }

            /**
             * Parses a variable input value into the scalar backing type.
             * @param input Raw variable value supplied by the client.
             * @param graphQLContext GraphQL request context.
             * @param locale Request locale.
             * @return Parsed scalar backing value.
             */
            override fun parseValue(
                input: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): LocalDate =
                when (input) {
                    is String -> runCatching { LocalDate.parse(input) }
                        .getOrElse { throw CoercingParseValueException("Invalid LocalDate: $input") }

                    else -> throw CoercingParseValueException("Expected String")
                }

            /**
             * Parses a string literal into the scalar backing type.
             * @param input AST literal supplied in the query.
             * @param variables Coerced query variables.
             * @param graphQLContext GraphQL request context.
             * @param locale Request locale.
             * @return Parsed scalar backing value.
             */
            override fun parseLiteral(
                input: Value<*>,
                variables: CoercedVariables,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): LocalDate =
                when (input) {
                    is StringValue -> runCatching { LocalDate.parse(input.value!!) }
                        .getOrElse {
                            throw CoercingParseLiteralException("Invalid LocalDate literal: ${input.value}")
                        }

                    else -> throw CoercingParseLiteralException("Expected StringValue")
                }
        })
        .build()
}
