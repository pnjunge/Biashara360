package com.app.biashara.utils

/**
 * Utility functions for safe logging that masks PII (Personally Identifiable Information).
 * Prevents sensitive data leakage in application logs.
 * 
 * 🔒 SECURITY: Always use these functions when logging user data.
 * 
 * ## Usage Guidelines:
 * 
 * ✅ DO:
 * - Use maskPhone() for phone numbers
 * - Use maskEmail() for email addresses
 * - Use maskName() for person names
 * - Use maskTransactionCode() for payment references
 * - Use redact() for passwords, tokens, API keys
 * - Use logSafe() for structured JSON logs
 * 
 * ❌ DON'T:
 * - Log raw phone numbers: println("Phone: $phone") ❌
 * - Log raw emails: logger.info("Email: $email") ❌
 * - Log passwords/tokens even masked: Use redact()
 * - Log full card numbers: Always use maskCardNumber()
 * 
 * ## Examples:
 * ```kotlin
 * // Bad
 * println("[AuthService] OTP sent to ${user.phone}")
 * 
 * // Good
 * println(LogUtils.logSafe(
 *     "otp_sent",
 *     "phone" to LogUtils.maskPhone(user.phone)
 * ))
 * ```
 */
object LogUtils {
    
    /**
     * Mask phone number showing only last 4 digits.
     * Examples:
     *   +254712345678 -> ****5678
     *   254712345678 -> ****5678
     *   0712345678 -> ****5678
     */
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return "****"
        val cleaned = phone.trim()
        return if (cleaned.length > 4) {
            "****" + cleaned.takeLast(4)
        } else {
            "****"
        }
    }
    
    /**
     * Mask email showing only first character and domain.
     * Examples:
     *   john.doe@example.com -> j***@example.com
     *   admin@biashara360.co.ke -> a***@biashara360.co.ke
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "***@***.***"
        val parts = email.trim().split("@")
        if (parts.size != 2) return "***@***.***"
        
        val localPart = parts[0]
        val domain = parts[1]
        
        val maskedLocal = if (localPart.isNotEmpty()) {
            localPart.first() + "***"
        } else {
            "***"
        }
        
        return "$maskedLocal@$domain"
    }
    
    /**
     * Mask name showing only first name initial and last name initial.
     * Examples:
     *   John Doe -> J. D.
     *   Amina Wanjiku Kamau -> A. W. K.
     */
    fun maskName(name: String?): String {
        if (name.isNullOrBlank()) return "***"
        val parts = name.trim().split(Regex("\\s+"))
        return parts.joinToString(" ") { part ->
            if (part.isNotEmpty()) "${part.first().uppercase()}." else ""
        }
    }
    
    /**
     * Mask transaction code showing only first 4 and last 4 characters.
     * Examples:
     *   QGR5TXH6YK -> QGR5***H6YK
     *   ABC123XYZ789 -> ABC1***789
     */
    fun maskTransactionCode(code: String?): String {
        if (code.isNullOrBlank()) return "****"
        val cleaned = code.trim()
        return when {
            cleaned.length <= 8 -> cleaned.take(4) + "****"
            else -> cleaned.take(4) + "***" + cleaned.takeLast(4)
        }
    }
    
    /**
     * Mask card number showing only last 4 digits.
     * Examples:
     *   4242424242424242 -> ************4242
     *   5555555555554444 -> ************4444
     */
    fun maskCardNumber(cardNumber: String?): String {
        if (cardNumber.isNullOrBlank()) return "************"
        val cleaned = cardNumber.trim().replace(Regex("[^0-9]"), "")
        return if (cleaned.length > 4) {
            "*".repeat(12) + cleaned.takeLast(4)
        } else {
            "*".repeat(12)
        }
    }
    
    /**
     * Mask order ID showing only first 8 characters (UUID prefix).
     * Examples:
     *   550e8400-e29b-41d4-a716-446655440000 -> 550e8400-****
     */
    fun maskOrderId(orderId: String?): String {
        if (orderId.isNullOrBlank()) return "****-****"
        val cleaned = orderId.trim()
        return if (cleaned.length > 8) {
            cleaned.take(8) + "-****"
        } else {
            cleaned + "-****"
        }
    }
    
    /**
     * Completely redact sensitive value (use for passwords, tokens, secrets).
     * Always returns [REDACTED] regardless of input.
     */
    fun redact(value: String?): String = "[REDACTED]"
    
    /**
     * Create a safe log message with masked PII.
     * Use for structured JSON logs.
     * 
     * Example:
     *   logSafe(
     *       "user_login_attempt",
     *       "email" to maskEmail(email),
     *       "phone" to maskPhone(phone)
     *   )
     */
    fun logSafe(event: String, vararg fields: Pair<String, Any?>): String {
        val fieldsJson = fields.joinToString(",") { (key, value) ->
            val safeValue = when (value) {
                null -> "null"
                is String -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"$value\""
            }
            "\"$key\":$safeValue"
        }
        return """{"event":"$event",$fieldsJson}"""
    }
}
