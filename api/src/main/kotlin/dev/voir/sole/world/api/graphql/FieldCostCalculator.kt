package dev.voir.sole.world.api.graphql

import graphql.analysis.FieldComplexityCalculator
import graphql.analysis.FieldComplexityEnvironment

/**
 * Scores a GraphQL document by the work it asks for rather than by how many fields it names.
 *
 * The default calculator counts one per field, which makes aliases free: twenty aliased
 * `listCities(page: {size: 1})` selections look like a trivial document and cost twenty scans of the
 * largest entity in the dataset. Weighting each field by the record set it may scan — declared in
 * the schema with `@cost` — puts the limit and the cost back in agreement.
 *
 * A field's cost is its own weight plus the cost of everything selected under it, so the weight is
 * charged once per selection. Nothing is multiplied by page size: every resolver reads an in-memory
 * index and returns a bounded page, so a larger page costs no more scans than a smaller one.
 */
class FieldCostCalculator : FieldComplexityCalculator {
    override fun calculate(environment: FieldComplexityEnvironment, childComplexity: Int): Int =
        weightOf(environment) + childComplexity

    /**
     * Reads a field's declared scan weight.
     * @param environment Field being scored.
     * @return Weight from the field's `@cost` directive, or [DEFAULT_WEIGHT] when it carries none.
     */
    private fun weightOf(environment: FieldComplexityEnvironment): Int {
        val directive = environment.fieldDefinition.getAppliedDirective(COST_DIRECTIVE)
            ?: return DEFAULT_WEIGHT

        return directive.getArgument(WEIGHT_ARGUMENT)?.getValue<Int>() ?: DEFAULT_WEIGHT
    }

    private companion object {
        const val COST_DIRECTIVE = "cost"
        const val WEIGHT_ARGUMENT = "weight"

        /** Fields that resolve from an identifier or an already-narrowed collection. */
        const val DEFAULT_WEIGHT = 1
    }
}
