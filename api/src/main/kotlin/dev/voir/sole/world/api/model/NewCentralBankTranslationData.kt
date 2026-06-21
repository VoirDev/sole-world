package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a central bank translation row.
 * @property centralBankId Parent central bank id.
 * @property name Localized display name.
 */
data class NewCentralBankTranslationData(
    val centralBankId: Int,
    val name: String,
)
