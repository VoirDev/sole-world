package dev.voir.sole.world.api.health

import dev.voir.sole.world.api.dataset.RawDataset
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Liveness and readiness endpoint, served without an API key.
 *
 * The dataset is parsed while the application context starts, so a served response is itself proof
 * that the dataset loaded: there is no partially-ready state to report.
 */
@RestController
class HealthController(
    private val dataset: RawDataset,
) {
    /**
     * Reports service health and which dataset version is loaded.
     * @return Health payload naming the dataset version.
     */
    @GetMapping("/healthz")
    fun health() = HealthResponse(
        status = "ok",
        datasetVersion = dataset.meta.version,
        datasetDate = dataset.meta.date,
    )
}

/**
 * Health payload.
 * @property status Fixed "ok" marker for readiness probes.
 * @property datasetVersion Version of the dataset held in memory.
 * @property datasetDate Publication timestamp of that dataset version.
 */
data class HealthResponse(
    val status: String,
    val datasetVersion: Int,
    val datasetDate: String,
)
