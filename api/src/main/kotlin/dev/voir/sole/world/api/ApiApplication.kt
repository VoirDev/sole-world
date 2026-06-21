package dev.voir.sole.world.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Documents the api application component used by the service.
 */
@SpringBootApplication
class ApiApplication

/**
 * Handles main work for this service component.
 * @param args Input value used by main.
 * @return The result produced by main.
 */
fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
