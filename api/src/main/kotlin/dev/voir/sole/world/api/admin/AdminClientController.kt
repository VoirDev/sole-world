package dev.voir.sole.world.api.admin

import dev.voir.sole.world.api.database.ClientRepository
import dev.voir.sole.world.api.model.ClientData
import dev.voir.sole.world.api.service.AccessKeyService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Admin-only REST API for managing client records and access keys. */
@RestController
@RequestMapping("/admin/api/clients")
class AdminClientController(
    private val clientRepository: ClientRepository,
    private val accessKeyService: AccessKeyService,
) {
    /** Lists all API clients without exposing raw access keys. */
    @GetMapping
    fun listClients(): List<ClientResponse> {
        return clientRepository.listClients().map(ClientResponse::from)
    }

    /** Creates a client and returns the raw access key exactly once. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createClient(@Valid @RequestBody request: ClientRequest): ClientSecretResponse {
        val key = accessKeyService.generate()
        val client = clientRepository.createClient(
            name = request.name.trim(),
            description = request.description?.trim()?.ifBlank { null },
            accessKeyHash = key.hash,
            accessKeyPrefix = key.prefix,
        )

        return ClientSecretResponse(
            client = ClientResponse.from(client),
            accessKey = key.raw,
        )
    }

    /** Updates client metadata without changing the access key. */
    @PutMapping("/{id}")
    fun updateClient(
        @PathVariable id: Int,
        @Valid @RequestBody request: ClientRequest,
    ): ClientResponse {
        val client = clientRepository.updateClient(
            id = id,
            name = request.name.trim(),
            description = request.description?.trim()?.ifBlank { null },
        ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")

        return ClientResponse.from(client)
    }

    /** Rotates the client's access key and returns the new raw key exactly once. */
    @PostMapping("/{id}/rotate-key")
    fun rotateKey(@PathVariable id: Int): ClientSecretResponse {
        val key = accessKeyService.generate()
        val client = clientRepository.rotateAccessKey(
            id = id,
            accessKeyHash = key.hash,
            accessKeyPrefix = key.prefix,
        ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")

        return ClientSecretResponse(
            client = ClientResponse.from(client),
            accessKey = key.raw,
        )
    }

    /** Removes a client and immediately invalidates its access key. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteClient(@PathVariable id: Int) {
        if (!clientRepository.deleteClient(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")
        }
    }
}

/** Request body for creating or updating a client. */
data class ClientRequest(
    @field:NotBlank
    val name: String,
    val description: String? = null,
)

/** Client details safe to show in admin responses. */
data class ClientResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val accessKeyPrefix: String,
    val active: Boolean,
) {
    companion object {
        fun from(client: ClientData): ClientResponse {
            return ClientResponse(
                id = client.id,
                name = client.name,
                description = client.description,
                accessKeyPrefix = client.accessKeyPrefix,
                active = client.active,
            )
        }
    }
}

/** Response returned when a newly generated raw access key must be shown once. */
data class ClientSecretResponse(
    val client: ClientResponse,
    val accessKey: String,
)
