package com.app.biashara.exceptions

import io.ktor.http.*

/**
 * Base exception for all application-level exceptions.
 * Provides structured error information for API responses.
 */
sealed class AppException(
    override val message: String,
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    val errorCode: String = "INTERNAL_ERROR",
    val details: Map<String, Any>? = null
) : Exception(message)

// ──── Authentication & Authorization ─────────────────────────────────────────

class UnauthorizedException(
    message: String = "Authentication required",
    errorCode: String = "UNAUTHORIZED",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.Unauthorized, errorCode, details)

class ForbiddenException(
    message: String = "Access denied",
    errorCode: String = "FORBIDDEN",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.Forbidden, errorCode, details)

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : AppException(message, HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS")

class TokenExpiredException(
    message: String = "Token has expired"
) : AppException(message, HttpStatusCode.Unauthorized, "TOKEN_EXPIRED")

class InvalidTokenException(
    message: String = "Invalid or malformed token"
) : AppException(message, HttpStatusCode.Unauthorized, "INVALID_TOKEN")

class OtpRequiredException(
    message: String = "OTP verification required"
) : AppException(message, HttpStatusCode.Unauthorized, "OTP_REQUIRED")

class InvalidOtpException(
    message: String = "Invalid or expired OTP"
) : AppException(message, HttpStatusCode.Unauthorized, "INVALID_OTP")

// ──── Validation ─────────────────────────────────────────────────────────────

class ValidationException(
    message: String = "Validation failed",
    val errors: List<ValidationError> = emptyList()
) : AppException(
    message = message,
    statusCode = HttpStatusCode.BadRequest,
    errorCode = "VALIDATION_ERROR",
    details = mapOf("errors" to errors)
)

data class ValidationError(
    val field: String,
    val message: String,
    val code: String? = null
)

// ──── Resource Not Found ─────────────────────────────────────────────────────

class NotFoundException(
    val resource: String,
    val identifier: String? = null
) : AppException(
    message = if (identifier != null) "$resource with ID '$identifier' not found" else "$resource not found",
    statusCode = HttpStatusCode.NotFound,
    errorCode = "NOT_FOUND",
    details = mapOf("resource" to resource, "identifier" to (identifier ?: ""))
)

// ──── Conflict & Duplicate ───────────────────────────────────────────────────

class ConflictException(
    message: String,
    errorCode: String = "CONFLICT",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.Conflict, errorCode, details)

class DuplicateResourceException(
    val resource: String,
    val field: String,
    val value: String
) : AppException(
    message = "$resource with $field '$value' already exists",
    statusCode = HttpStatusCode.Conflict,
    errorCode = "DUPLICATE_RESOURCE",
    details = mapOf("resource" to resource, "field" to field, "value" to value)
)

// ──── Business Logic ─────────────────────────────────────────────────────────

class BusinessRuleException(
    message: String,
    errorCode: String = "BUSINESS_RULE_VIOLATION",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.BadRequest, errorCode, details)

class InsufficientStockException(
    val productId: String,
    val productName: String,
    val requested: Int,
    val available: Int
) : AppException(
    message = "Insufficient stock for '$productName'. Requested: $requested, Available: $available",
    statusCode = HttpStatusCode.BadRequest,
    errorCode = "INSUFFICIENT_STOCK",
    details = mapOf(
        "productId" to productId,
        "productName" to productName,
        "requested" to requested,
        "available" to available
    )
)

class InvalidStatusTransitionException(
    val entity: String,
    val currentStatus: String,
    val targetStatus: String
) : AppException(
    message = "Cannot transition $entity from '$currentStatus' to '$targetStatus'",
    statusCode = HttpStatusCode.BadRequest,
    errorCode = "INVALID_STATUS_TRANSITION",
    details = mapOf(
        "entity" to entity,
        "currentStatus" to currentStatus,
        "targetStatus" to targetStatus
    )
)

// ──── Payment & Financial ────────────────────────────────────────────────────

class PaymentException(
    message: String,
    errorCode: String = "PAYMENT_ERROR",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.BadRequest, errorCode, details)

class PaymentAlreadyProcessedException(
    val orderId: String
) : AppException(
    message = "Payment for order '$orderId' has already been processed",
    statusCode = HttpStatusCode.Conflict,
    errorCode = "PAYMENT_ALREADY_PROCESSED",
    details = mapOf("orderId" to orderId)
)

class InvalidAmountException(
    message: String = "Invalid amount",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.BadRequest, "INVALID_AMOUNT", details)

// ──── External Services ──────────────────────────────────────────────────────

open class ExternalServiceException(
    val service: String,
    message: String,
    errorCode: String = "EXTERNAL_SERVICE_ERROR",
    details: Map<String, Any>? = null
) : AppException(
    message = "$service error: $message",
    statusCode = HttpStatusCode.BadGateway,
    errorCode = errorCode,
    details = (details ?: emptyMap()) + mapOf("service" to service)
)

class MpesaException(
    message: String,
    details: Map<String, Any>? = null
) : ExternalServiceException("Mpesa", message, "MPESA_ERROR", details)

class CyberSourceException(
    message: String,
    details: Map<String, Any>? = null
) : ExternalServiceException("CyberSource", message, "CYBERSOURCE_ERROR", details)

class KraEtimsException(
    message: String,
    details: Map<String, Any>? = null
) : ExternalServiceException("KRA eTIMS", message, "KRA_ETIMS_ERROR", details)

class SmsServiceException(
    message: String,
    details: Map<String, Any>? = null
) : ExternalServiceException("SMS Service", message, "SMS_SERVICE_ERROR", details)

class EmailServiceException(
    message: String,
    details: Map<String, Any>? = null
) : ExternalServiceException("Email Service", message, "EMAIL_SERVICE_ERROR", details)

// ──── Rate Limiting ──────────────────────────────────────────────────────────

class RateLimitExceededException(
    message: String = "Too many requests. Please try again later.",
    val retryAfterSeconds: Long? = null
) : AppException(
    message = message,
    statusCode = HttpStatusCode.TooManyRequests,
    errorCode = "RATE_LIMIT_EXCEEDED",
    details = retryAfterSeconds?.let { mapOf("retryAfterSeconds" to it) }
)

// ──── Configuration ──────────────────────────────────────────────────────────

class ConfigurationException(
    message: String,
    errorCode: String = "CONFIGURATION_ERROR",
    details: Map<String, Any>? = null
) : AppException(message, HttpStatusCode.InternalServerError, errorCode, details)

class MissingConfigurationException(
    val configKey: String
) : AppException(
    message = "Required configuration '$configKey' is missing",
    statusCode = HttpStatusCode.InternalServerError,
    errorCode = "MISSING_CONFIGURATION",
    details = mapOf("configKey" to configKey)
)
