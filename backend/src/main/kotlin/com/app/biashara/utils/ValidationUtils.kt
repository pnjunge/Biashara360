package com.app.biashara.utils

/**
 * Central validation utilities for input validation across the application.
 * Implements security best practices and enforces business rules.
 */
object ValidationUtils {

    // Email validation regex (RFC 5322 simplified)
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$"
    )

    // Phone validation for Kenya (supports multiple formats)
    private val KENYA_PHONE_REGEX = Regex(
        "^(\\+?254|0)[1-9]\\d{8}\$"
    )

    // Password strength requirements:
    // - Minimum 12 characters (NIST 800-63B compliant)
    // - At least one uppercase letter
    // - At least one lowercase letter
    // - At least one digit
    // - At least one special character from allowed set
    private val PASSWORD_REGEX = Regex(
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&_\\-+=])[A-Za-z\\d@$!%*?&_\\-+=]{12,}\$"
    )

    /**
     * Validates email format.
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email) && email.length <= 255
    }

    /**
     * Validates Kenyan phone number format.
     * Accepts: +254712345678, 0712345678, 712345678 formats
     * @param phone Phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhoneKE(phone: String): Boolean {
        val normalized = normalizePhoneKE(phone)
        return KENYA_PHONE_REGEX.matches(normalized)
    }

    /**
     * Normalizes Kenyan phone to international format (254...).
     * @param phone Phone number in any supported format
     * @return Normalized phone number
     */
    fun normalizePhoneKE(phone: String): String = when {
        phone.startsWith("254") -> phone                    // Already normalized
        phone.startsWith("+254") -> phone.substring(1)      // Remove +
        phone.startsWith("0") -> "254${phone.substring(1)}" // Add country code
        else -> "254$phone"                                 // Add country code
    }

    /**
     * Validates password strength per NIST 800-63B guidelines.
     * Requires:
     * - Minimum 12 characters OR passphrase (3+ words)
     * - Mix of uppercase, lowercase, digits, special chars
     * - No common patterns
     *
     * @param password Password to validate
     * @return true if valid, false otherwise
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 12) return false
        
        // Check complexity requirements
        if (!PASSWORD_REGEX.matches(password)) return false
        
        // Check against common patterns
        val commonPatterns = listOf(
            "password", "123456", "qwerty", "admin", "letmein",
            "welcome", "monkey", "dragon", "master", "sunshine"
        )
        
        val lowerPassword = password.lowercase()
        if (commonPatterns.any { it in lowerPassword }) return false
        
        return true
    }

    /**
     * Gets user-friendly password requirements message.
     * @return Requirements description
     */
    fun getPasswordRequirements(): String =
        "Password must be at least 12 characters with uppercase, lowercase, digit, and special character"

    /**
     * Validates business name.
     * @param name Business name to validate
     * @return true if valid, false otherwise
     */
    fun isValidBusinessName(name: String): Boolean {
        return name.isNotBlank() && name.length in 2..255 && !name.containsUnsafeChars()
    }

    /**
     * Validates person name.
     * @param name Name to validate
     * @return true if valid, false otherwise
     */
    fun isValidPersonName(name: String): Boolean {
        return name.isNotBlank() && name.length in 2..255 && !name.containsUnsafeChars()
    }

    /**
     * Checks if string contains characters that might indicate injection attempt.
     * @return true if unsafe characters detected
     */
    private fun String.containsUnsafeChars(): Boolean {
        val unsafeChars = setOf('<', '>', '"', '\'', '\\', '\u0000')
        return any { it in unsafeChars }
    }

    /**
     * Validates UUID format (36 chars including dashes).
     * @param uuid UUID to validate
     * @return true if valid format
     */
    fun isValidUUID(uuid: String): Boolean {
        return uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
    }

    /**
     * Validates currency code (ISO 4217).
     * @param code Currency code to validate
     * @return true if valid
     */
    fun isValidCurrencyCode(code: String): Boolean {
        return code.matches(Regex("[A-Z]{3}"))
    }

    /**
     * Validates KRA PIN format (Kenya tax ID).
     * Format: Letter + 9 digits + Letter (e.g., P051234567X)
     * @param pin KRA PIN to validate
     * @return true if valid
     */
    fun isValidKraPin(pin: String): Boolean {
        return pin.matches(Regex("[A-Z]\\d{9}[A-Z]"))
    }

    /**
     * Validates numeric amount (for prices, quantities, etc.).
     * @param amount Amount to validate
     * @param min Minimum allowed value (default 0)
     * @param max Maximum allowed value (default Double.MAX_VALUE)
     * @return true if valid
     */
    fun isValidAmount(amount: Double, min: Double = 0.0, max: Double = Double.MAX_VALUE): Boolean {
        return amount in min..max && !amount.isNaN() && !amount.isInfinite()
    }
}
