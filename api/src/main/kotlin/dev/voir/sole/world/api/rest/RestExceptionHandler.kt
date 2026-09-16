package dev.voir.sole.world.api.rest

import dev.voir.sole.world.openapi.model.Problem
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Renders REST failures as RFC 9457 problem documents.
 *
 * Client mistakes are answered with enough detail to correct the request, and nothing else: no
 * exception messages, stack traces or internal identifiers reach the caller. Unexpected failures
 * are logged once here and returned as a bare 500.
 *
 * Some of that detail is the caller's own input echoed back — an unknown `include` value is named so
 * they can see what they got wrong, and the request URI appears in `instance`. That is deliberate,
 * and it is safe because the document is served as `application/problem+json` with
 * `X-Content-Type-Options: nosniff`, so a browser will not be talked into treating it as markup.
 */
@RestControllerAdvice(basePackages = ["dev.voir.sole.world.api"])
class RestExceptionHandler {
    /** Answers an unknown `include` with the list of names the endpoint does accept. */
    @ExceptionHandler(UnknownIncludeException::class)
    fun onUnknownInclude(
        failure: UnknownIncludeException,
        request: HttpServletRequest,
    ): ResponseEntity<Problem> {
        return problem(
            status = HttpStatus.BAD_REQUEST,
            title = "Unknown include",
            detail = "Unknown include ${failure.unknown.joinToString(", ")}.",
            request = request,
            allowed = failure.allowed.sorted(),
        )
    }

    /** Answers an unknown `sort` or `order` with the list of values the endpoint does accept. */
    @ExceptionHandler(UnknownSortException::class)
    fun onUnknownSort(
        failure: UnknownSortException,
        request: HttpServletRequest,
    ): ResponseEntity<Problem> {
        return problem(
            status = HttpStatus.BAD_REQUEST,
            title = "Unknown sort",
            detail = "Unknown ${failure.parameter} '${failure.unknown}'.",
            request = request,
            allowed = failure.allowed,
        )
    }

    /** Answers a resource that does not exist. */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun onNotFound(
        failure: ResourceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<Problem> {
        return problem(
            status = HttpStatus.NOT_FOUND,
            title = "Not found",
            detail = failure.detail,
            request = request,
        )
    }

    /** Answers a parameter that violates the documented contract. */
    @ExceptionHandler(
        IllegalArgumentException::class,
        MethodArgumentTypeMismatchException::class,
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
        ConstraintViolationException::class,
        MissingServletRequestParameterException::class,
    )
    fun onInvalidRequest(failure: Exception, request: HttpServletRequest): ResponseEntity<Problem> {
        return problem(
            status = HttpStatus.BAD_REQUEST,
            title = "Invalid request",
            detail = detailFor(failure),
            request = request,
        )
    }

    /** Answers anything unexpected without leaking why. */
    @ExceptionHandler(Exception::class)
    fun onUnexpectedFailure(failure: Exception, request: HttpServletRequest): ResponseEntity<Problem> {
        log.error("Unhandled failure serving {}", request.requestURI, failure)

        return problem(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal server error",
            detail = "The request could not be completed.",
            request = request,
        )
    }

    /**
     * Produces a caller-safe explanation.
     *
     * Only messages this application wrote are passed through. Framework and library messages can
     * carry types, parameter internals and values from the request, so they are replaced.
     */
    private fun detailFor(failure: Exception): String = when (failure) {
        is ConstraintViolationException ->
            failure.constraintViolations
                .joinToString("; ") { violation ->
                    // The property path is "method.parameter"; only the parameter matters to a caller.
                    "${violation.propertyPath.toString().substringAfterLast('.')} ${violation.message}"
                }
                .ifEmpty { "One or more parameters are outside their documented range." }

        is IllegalArgumentException -> failure.message ?: "The request is not valid."
        is MethodArgumentTypeMismatchException ->
            "Parameter '${failure.name}' is not in the expected format."
        is MissingServletRequestParameterException ->
            "Parameter '${failure.parameterName}' is required."
        else -> "One or more parameters are outside their documented range."
    }

    private fun problem(
        status: HttpStatus,
        title: String,
        detail: String,
        request: HttpServletRequest,
        allowed: List<String>? = null,
    ): ResponseEntity<Problem> {
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                Problem(
                    type = "about:blank",
                    title = title,
                    status = status.value(),
                    detail = detail,
                    instance = request.requestURI,
                    allowed = allowed,
                ),
            )
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RestExceptionHandler::class.java)
    }
}

/**
 * Raised when a path identifier matches no record.
 * @property detail Caller-safe explanation naming the resource kind and identifier.
 */
class ResourceNotFoundException(val detail: String) : RuntimeException(detail) {
    companion object {
        /**
         * Builds a not-found failure for a resource kind and identifier.
         * @param resource Human-readable resource name, such as "Country".
         * @param identifier Identifier the caller supplied.
         * @return Failure carrying a caller-safe message.
         */
        fun of(resource: String, identifier: Any): ResourceNotFoundException =
            ResourceNotFoundException("$resource '$identifier' does not exist.")
    }
}
