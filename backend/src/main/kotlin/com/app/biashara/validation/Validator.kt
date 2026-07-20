package com.app.biashara.validation

import com.app.biashara.exceptions.ValidationError
import com.app.biashara.exceptions.ValidationException
import com.app.biashara.utils.ValidationUtils

/**
 * Fluent validation builder for request validation.
 * Usage:
 * ```
 * Validator.validate {
 *     field("email", email) {
 *         required()
 *         email()
 *     }
 *     field("password", password) {
 *         required()
 *         password()
 *     }
 * }
 * ```
 */
class Validator {
    private val errors = mutableListOf<ValidationError>()

    fun field(fieldName: String, value: Any?, block: FieldValidator.() -> Unit) {
        val fieldValidator = FieldValidator(fieldName, value)
        fieldValidator.block()
        errors.addAll(fieldValidator.errors)
    }

    fun throwIfInvalid() {
        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed", errors)
        }
    }

    companion object {
        fun validate(block: Validator.() -> Unit) {
            val validator = Validator()
            validator.block()
            validator.throwIfInvalid()
        }
    }
}

class FieldValidator(
    private val fieldName: String,
    private val value: Any?
) {
    val errors = mutableListOf<ValidationError>()

    private fun addError(message: String, code: String? = null) {
        errors.add(ValidationError(fieldName, message, code))
    }

    // ──── Basic Validations ──────────────────────────────────────────────────

    fun required() = apply {
        when (value) {
            null -> addError("$fieldName is required", "REQUIRED")
            is String -> if (value.isBlank()) addError("$fieldName cannot be blank", "REQUIRED")
            is Collection<*> -> if (value.isEmpty()) addError("$fieldName cannot be empty", "REQUIRED")
        }
    }

    fun optional(block: FieldValidator.() -> Unit) = apply {
        if (value != null && (value !is String || value.isNotBlank())) {
            block()
        }
    }

    // ──── String Validations ─────────────────────────────────────────────────

    fun minLength(min: Int) = apply {
        if (value is String && value.length < min) {
            addError("$fieldName must be at least $min characters", "MIN_LENGTH")
        }
    }

    fun maxLength(max: Int) = apply {
        if (value is String && value.length > max) {
            addError("$fieldName must not exceed $max characters", "MAX_LENGTH")
        }
    }

    fun length(min: Int, max: Int) = apply {
        minLength(min)
        maxLength(max)
    }

    fun email() = apply {
        if (value is String && !ValidationUtils.isValidEmail(value)) {
            addError("$fieldName must be a valid email address", "INVALID_EMAIL")
        }
    }

    fun phone() = apply {
        if (value is String && !ValidationUtils.isValidPhoneKE(value)) {
            addError("$fieldName must be a valid Kenyan phone number", "INVALID_PHONE")
        }
    }

    fun password() = apply {
        if (value is String && !ValidationUtils.isValidPassword(value)) {
            addError(ValidationUtils.getPasswordRequirements(), "WEAK_PASSWORD")
        }
    }

    fun uuid() = apply {
        if (value is String && !ValidationUtils.isValidUUID(value)) {
            addError("$fieldName must be a valid UUID", "INVALID_UUID")
        }
    }

    fun kraPin() = apply {
        if (value is String && !ValidationUtils.isValidKraPin(value)) {
            addError("$fieldName must be a valid KRA PIN (e.g., P051234567X)", "INVALID_KRA_PIN")
        }
    }

    fun matches(regex: Regex, errorMessage: String? = null) = apply {
        if (value is String && !value.matches(regex)) {
            addError(errorMessage ?: "$fieldName has invalid format", "INVALID_FORMAT")
        }
    }

    fun alphanumeric() = apply {
        if (value is String && !value.matches(Regex("^[a-zA-Z0-9]+$"))) {
            addError("$fieldName must contain only letters and numbers", "INVALID_FORMAT")
        }
    }

    // ──── Numeric Validations ────────────────────────────────────────────────

    fun positive() = apply {
        when (value) {
            is Number -> if (value.toDouble() <= 0) {
                addError("$fieldName must be positive", "INVALID_NUMBER")
            }
        }
    }

    fun nonNegative() = apply {
        when (value) {
            is Number -> if (value.toDouble() < 0) {
                addError("$fieldName must be non-negative", "INVALID_NUMBER")
            }
        }
    }

    fun min(min: Double) = apply {
        when (value) {
            is Number -> if (value.toDouble() < min) {
                addError("$fieldName must be at least $min", "BELOW_MINIMUM")
            }
        }
    }

    fun max(max: Double) = apply {
        when (value) {
            is Number -> if (value.toDouble() > max) {
                addError("$fieldName must not exceed $max", "ABOVE_MAXIMUM")
            }
        }
    }

    fun range(min: Double, max: Double) = apply {
        this.min(min)
        this.max(max)
    }

    fun validAmount(min: Double = 0.0, max: Double = Double.MAX_VALUE) = apply {
        when (value) {
            is Number -> {
                val amount = value.toDouble()
                if (!ValidationUtils.isValidAmount(amount, min, max)) {
                    addError("$fieldName must be between $min and $max", "INVALID_AMOUNT")
                }
            }
        }
    }

    // ──── Collection Validations ─────────────────────────────────────────────

    fun notEmpty() = apply {
        when (value) {
            is Collection<*> -> if (value.isEmpty()) {
                addError("$fieldName must not be empty", "EMPTY_COLLECTION")
            }
            is String -> if (value.isEmpty()) {
                addError("$fieldName must not be empty", "EMPTY_STRING")
            }
        }
    }

    fun minSize(min: Int) = apply {
        when (value) {
            is Collection<*> -> if (value.size < min) {
                addError("$fieldName must contain at least $min items", "MIN_SIZE")
            }
        }
    }

    fun maxSize(max: Int) = apply {
        when (value) {
            is Collection<*> -> if (value.size > max) {
                addError("$fieldName must not contain more than $max items", "MAX_SIZE")
            }
        }
    }

    // ──── Enum Validations ───────────────────────────────────────────────────

    fun oneOf(vararg allowedValues: String) = apply {
        if (value is String && value !in allowedValues) {
            addError("$fieldName must be one of: ${allowedValues.joinToString(", ")}", "INVALID_OPTION")
        }
    }

    fun <T : Enum<T>> enumValue(enumClass: Class<T>) = apply {
        if (value is String) {
            try {
                java.lang.Enum.valueOf(enumClass, value.uppercase())
            } catch (e: IllegalArgumentException) {
                val validValues = enumClass.enumConstants.joinToString(", ") { it.name }
                addError("$fieldName must be one of: $validValues", "INVALID_ENUM")
            }
        }
    }

    // ──── Custom Validations ─────────────────────────────────────────────────

    fun custom(errorMessage: String, code: String = "CUSTOM_VALIDATION", predicate: (Any?) -> Boolean) = apply {
        if (!predicate(value)) {
            addError(errorMessage, code)
        }
    }
}

/**
 * Extension function for convenient validation in route handlers.
 */
fun <T> T.validate(block: Validator.() -> Unit): T {
    Validator.validate(block)
    return this
}
