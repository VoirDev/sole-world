package dev.voir.sole.world.api

import dev.voir.sole.world.api.service.SeedService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Runs required bootstrap work before the application is reported ready.
 * @property seedService Value supplied for seedService.
 */
@Component
class Bootstrap(
    private val seedService: SeedService,
    @Value("\${seed.import-enabled:true}")
    private val importEnabled: Boolean,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(Bootstrap::class.java)

    /** Imports bundled data before the application can be considered ready. */
    override fun run(args: ApplicationArguments) {
        if (!importEnabled) {
            log.info("Seed import disabled")
            return
        }

        log.info("Seed import started")
        seedService.import()
        log.info("Seed import finished")
    }
}
