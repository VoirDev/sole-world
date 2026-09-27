package dev.voir.sole.world.api.dataset

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Runs the startup integrity check over the dataset this repository ships.
 *
 * Every other test runs against a small fixture, so a broken reference or two aliases that collide
 * in `data/` would pass the suite and only fail when the image starts. Identifiers are written by
 * hand where no standard code exists, which makes that the likelier mistake.
 */
class BundledDatasetTest {
    @Test
    fun `the bundled dataset is internally consistent`() {
        assertDoesNotThrow { DatasetLoader(dataRoot = "data").rawDataset() }
    }
}
