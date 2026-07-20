package com.app.biashara.plugins

import com.app.biashara.exceptions.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

import kotlinx.serialization.json.*

private val logger = LoggerFactory.getLogger("ExceptionHandler")

// Recursive helper to convert arbitrary JVM Map/List/value to JsonElement
fun anyToJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Map<*, *> -> {
            val map = mutableMapOf<String, JsonElement>()
            value.forEach { (k, v) ->
                map[k.toString()] = anyToJsonElement(v)
            }
            JsonObject(map)
        }
        is Iterable<*> -> {
            val list = value.map { anyToJsonElement(it) }
            JsonArray(list)
        }
        is Array<*> -> {
            val list = value.map { anyToJsonElement(it) }
            JsonArray(list)
        }
        else -> JsonPrimitive(value.toString())
    }
}

/**
 * Standardized error response structure for all API errors.
 */
@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: JsonElement? = null,
    val timestamp: String = java.time.Instant.now().toString()
)

/**
 * Configures global exception handling for the application.
 * Catches all exceptions and returns structured error responses.
 */
fun Application.configureExceptionHandling() {
    install(StatusPages) {
        // Handle custom application exceptions
        exception<AppException> { call, cause ->
            logger.warn("Application exception: ${cause.errorCode} - ${cause.message}", cause)
            
            call.respond(
                cause.statusCode,
                ErrorResponse(
                    error = ErrorDetail(
                        code = cause.errorCode,
                        message = cause.message,
                        details = cause.details?.let { anyToJsonElement(it) }
                    )
                )
            )
        }

        // Handle validation exceptions with field-level errors
        exception<ValidationException> { call, cause ->
            logger.info("Validation failed: ${cause.errors.size} errors")
            
            val errorDetails = mapOf(
                "fields" to cause.errors.map { 
                    mapOf(
                        "field" to it.field,
                        "message" to it.message,
                        "code" to (it.code ?: "VALIDATION_ERROR")
                    )
                }
            )
            
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "VALIDATION_ERROR",
                        message = cause.message,
                        details = anyToJsonElement(errorDetails)
                    )
                )
            )
        }

        // Handle malformed JSON
        exception<kotlinx.serialization.SerializationException> { call, cause ->
            logger.warn("JSON serialization error: ${cause.message}")
            
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_JSON",
                        message = "Invalid request format: ${cause.message}",
                        details = null
                    )
                )
            )
        }

        // Handle missing request body
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            logger.warn("Bad request: ${cause.message}")
            
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "BAD_REQUEST",
                        message = cause.message ?: "Invalid request",
                        details = null
                    )
                )
            )
        }

        // Handle database exceptions
        exception<org.jetbrains.exposed.exceptions.ExposedSQLException> { call, cause ->
            logger.error("Database error: ${cause.message}", cause)
            
            // Check for specific SQL errors
            val (code, message) = when {
                cause.message?.contains("unique constraint", ignoreCase = true) == true ->
                    "DUPLICATE_RESOURCE" to "Resource already exists"
                cause.message?.contains("foreign key constraint", ignoreCase = true) == true ->
                    "CONSTRAINT_VIOLATION" to "Cannot complete operation: related resource not found"
                else ->
                    "DATABASE_ERROR" to "A database error occurred"
            }
            
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ErrorDetail(
                        code = code,
                        message = message,
                        details = null
                    )
                )
            )
        }

        // Handle number format exceptions
        exception<NumberFormatException> { call, cause ->
            logger.warn("Number format error: ${cause.message}")
            
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_NUMBER_FORMAT",
                        message = "Invalid number format in request",
                        details = null
                    )
                )
            )
        }

        // Handle null pointer exceptions (shouldn't happen, but safety net)
        exception<NullPointerException> { call, cause ->
            logger.error("Null pointer exception: ${cause.message}", cause)
            
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INTERNAL_ERROR",
                        message = "An internal error occurred",
                        details = null
                    )
                )
            )
        }

        // Handle illegal argument exceptions
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Illegal argument: ${cause.message}")
            
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_ARGUMENT",
                        message = cause.message ?: "Invalid argument provided",
                        details = null
                    )
                )
            )
        }

        // Catch-all for unexpected exceptions
        exception<Throwable> { call, cause ->
            logger.error("Unexpected error: ${cause.message}", cause)
            
            // Don't expose internal error details in production
            val isDevelopment = System.getenv("ENVIRONMENT") != "production"
            
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INTERNAL_ERROR",
                        message = "An unexpected error occurred",
                        details = if (isDevelopment) {
                            anyToJsonElement(mapOf(
                                "exception" to cause::class.simpleName,
                                "message" to (cause.message ?: "No message")
                            ))
                        } else null
                    )
                )
            )
        }

        // Handle 404 - Not Found (route level)
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "ROUTE_NOT_FOUND",
                        message = "The requested endpoint does not exist",
                        details = anyToJsonElement(mapOf("path" to call.request.local.uri))
                    )
                )
            )
        }

        // Handle 405 - Method Not Allowed
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "METHOD_NOT_ALLOWED",
                        message = "HTTP method not allowed for this endpoint",
                        details = anyToJsonElement(mapOf(
                            "method" to call.request.local.method.value,
                            "path" to call.request.local.uri
                        ))
                    )
                )
            )
        }
    }
}
