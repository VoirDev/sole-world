package dev.voir.sole.world.api.graphql

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import kotlinx.datetime.LocalDate
import java.util.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Registers custom scalar coercions used by the GraphQL schema. */
@DgsComponent
class GraphQLScalarConfig {
    /**
     * Adds extended and Kotlin-specific scalar types to DGS runtime wiring.
     * @param builder Runtime wiring builder supplied by DGS.
     * @return Builder with scalar registrations applied.
     */
    @DgsRuntimeWiring
    fun addScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder
            .scalar(ExtendedScalars.Json)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.GraphQLByte)
            .scalar(instantScalar)
            .scalar(dateScalar)
            .scalar(uuidScalar)
    }

    private val instantScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Instant")
        .description("kotlin.time.Instant scalar")
        .coercing(object : Coercing<Instant, String> {
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
                locale: Locale
            ): String? =
                when (dataFetcherResult) {
                    is Instant -> dataFetcherResult.toString()
                    else -> throw CoercingSerializeException("Expected kotlin.time.Instant")
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
                locale: Locale
            ): Instant =
                when (input) {
                    is String -> runCatching { Instant.parse(input) }
                        .getOrElse { throw CoercingParseValueException("Invalid Instant: $input") }

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
                locale: Locale
            ): Instant =
                when (input) {
                    is StringValue -> runCatching { Instant.parse(input.value!!) }
                        .getOrElse { throw CoercingParseLiteralException("Invalid Instant literal: ${input.value}") }

                    else -> throw CoercingParseLiteralException("Expected StringValue")
                }
        })
        .build()

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
                locale: Locale
            ): String? =
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
                locale: Locale
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
                locale: Locale
            ): LocalDate =
                when (input) {
                    is StringValue -> runCatching { LocalDate.parse(input.value!!) }
                        .getOrElse { throw CoercingParseLiteralException("Invalid LocalDate literal: ${input.value}") }

                    else -> throw CoercingParseLiteralException("Expected StringValue")
                }
        })
        .build()

    private val uuidScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Uuid")
        .description("kotlinx.uuid.Uuid scalar")
        .coercing(object : Coercing<Uuid, String> {
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
                locale: Locale
            ): String {
                val value = dataFetcherResult as? Uuid
                    ?: throw CoercingSerializeException("Expected Uuid but was ${dataFetcherResult::class.qualifiedName}")
                return value.toString()
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
                locale: Locale
            ): Uuid {
                val text = input.toString()
                return try {
                    Uuid.parse(text)
                } catch (e: IllegalArgumentException) {
                    throw CoercingParseValueException("Invalid Uuid value '$text'", e)
                }
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
                locale: Locale
            ): Uuid {
                val value = (input as? StringValue)?.value
                    ?: throw CoercingParseLiteralException("Expected StringValue literal for Uuid")
                return try {
                    Uuid.parse(value)
                } catch (e: IllegalArgumentException) {
                    throw CoercingParseLiteralException("Invalid Uuid literal '$value'", e)
                }
            }
        })
        .build()
}
