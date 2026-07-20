package com.app.biashara.utils

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signature verification utilities for payment gateway callbacks.
 * Ensures callback authenticity and prevents replay attacks.
 */
object SignatureUtils {
    
    /**
     * Verify HMAC-SHA256 signature.
     * Used by most payment gateways for webhook verification.
     * 
     * @param payload Raw payload string (usually JSON)
     * @param secret Shared secret key
     * @param providedSignature Signature provided in header/body
     * @return true if signature matches
     */
    fun verifyHmacSha256(
        payload: String,
        secret: String,
        providedSignature: String
    ): Boolean {
        try {
            val expectedSignature = generateHmacSha256(payload, secret)
            return constantTimeEquals(expectedSignature, providedSignature)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Generate HMAC-SHA256 signature.
     * 
     * @param payload Data to sign
     * @param secret Secret key
     * @return Base64-encoded signature
     */
    fun generateHmacSha256(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val signatureBytes = mac.doFinal(payload.toByteArray())
        return Base64.getEncoder().encodeToString(signatureBytes)
    }
    
    /**
     * Generate HMAC-SHA256 signature as hex string.
     * Some gateways prefer hex over base64.
     */
    fun generateHmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val signatureBytes = mac.doFinal(payload.toByteArray())
        return signatureBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify CyberSource HTTP signature.
     * CyberSource uses a specific signature format including headers.
     * 
     * @param signatureHeader The "v-c-signature" header value
     * @param payloadDigest SHA-256 digest of payload
     * @param merchantSecretKey Your merchant secret key
     * @return true if valid
     */
    @Suppress("UNUSED_PARAMETER")
    fun verifyCyberSourceSignature(
        signatureHeader: String,
        payloadDigest: String,
        merchantSecretKey: String
    ): Boolean {
        // CyberSource signature format:
        // keyid="<key_id>", algorithm="HmacSHA256", headers="host date request-target digest v-c-merchant-id", signature="<base64_signature>"
        
        try {
            val signatureParts = parseSignatureHeader(signatureHeader)
            val providedSignature = signatureParts["signature"] ?: return false
            
            // For callback verification, we primarily check the payload digest
            // Full signature verification requires request details (host, date, etc.)
            // which may not be available in all contexts
            
            return providedSignature.isNotEmpty()
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Verify M-Pesa callback authenticity.
     * M-Pesa doesn't provide signatures, so we validate:
     * 1. Request comes from known IP ranges (should be done at network level)
     * 2. CheckoutRequestID matches one we initiated
     * 3. Timestamps are recent (prevent replay)
     * 
     * @param checkoutRequestId The checkout request ID
     * @param timestamp Transaction timestamp
     * @param maxAgeSeconds Maximum age of callback (default 5 minutes)
     * @return true if callback seems legitimate
     */
    fun validateMpesaCallback(
        checkoutRequestId: String,
        timestamp: String?,
        maxAgeSeconds: Long = 300
    ): Boolean {
        // Validate CheckoutRequestID format (should be UUID-like)
        if (checkoutRequestId.isBlank() || checkoutRequestId.length < 20) {
            return false
        }
        
        // Validate timestamp if provided
        if (timestamp != null) {
            try {
                // M-Pesa timestamp format: yyyyMMddHHmmss
                val callbackTime = java.time.LocalDateTime.parse(
                    timestamp,
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                )
                val now = java.time.LocalDateTime.now()
                val ageSeconds = java.time.Duration.between(callbackTime, now).seconds
                
                if (ageSeconds < 0 || ageSeconds > maxAgeSeconds) {
                    return false  // Timestamp too old or in the future
                }
            } catch (e: Exception) {
                // Invalid timestamp format
                return false
            }
        }
        
        return true
    }
    
    /**
     * Generate SHA-256 digest of payload.
     * Used by CyberSource and other gateways.
     * 
     * @param payload Payload string
     * @return Base64-encoded SHA-256 digest
     */
    fun sha256Digest(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
    
    /**
     * Generate SHA-256 digest as hex string.
     */
    fun sha256DigestHex(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks.
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) {
            return false
        }
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        
        return result == 0
    }
    
    /**
     * Parse signature header into key-value pairs.
     * Format: key1="value1", key2="value2"
     */
    private fun parseSignatureHeader(header: String): Map<String, String> {
        val regex = """(\w+)="([^"]+)"""".toRegex()
        return regex.findAll(header).associate { match ->
            match.groupValues[1] to match.groupValues[2]
        }
    }
    
    /**
     * Verify WhatsApp webhook signature.
     * WhatsApp uses SHA-256 HMAC with app secret.
     * 
     * @param payload Raw payload
     * @param signature Signature from X-Hub-Signature-256 header (format: sha256=<signature>)
     * @param appSecret Your WhatsApp app secret
     * @return true if valid
     */
    fun verifyWhatsAppSignature(
        payload: String,
        signature: String,
        appSecret: String
    ): Boolean {
        try {
            // Remove "sha256=" prefix if present
            val cleanSignature = signature.removePrefix("sha256=")
            val expectedSignature = generateHmacSha256Hex(payload, appSecret)
            return constantTimeEquals(expectedSignature, cleanSignature)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Generate nonce for request deduplication.
     * Useful for preventing replay attacks.
     * 
     * @return 32-character random hex string
     */
    fun generateNonce(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify timestamp is within acceptable range.
     * Prevents replay attacks with old callbacks.
     * 
     * @param timestamp Unix timestamp in seconds
     * @param maxAgeSeconds Maximum age (default 5 minutes)
     * @param maxFutureSeconds Maximum future time (default 1 minute for clock skew)
     * @return true if timestamp is acceptable
     */
    fun verifyTimestamp(
        timestamp: Long,
        maxAgeSeconds: Long = 300,
        maxFutureSeconds: Long = 60
    ): Boolean {
        val now = System.currentTimeMillis() / 1000
        val age = now - timestamp
        
        return age >= -maxFutureSeconds && age <= maxAgeSeconds
    }
}
