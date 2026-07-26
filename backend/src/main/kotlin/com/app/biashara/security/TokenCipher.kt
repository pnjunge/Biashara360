package com.app.biashara.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class TokenCipher(encodedKey: String) {
    private val key: ByteArray = runCatching { Base64.getDecoder().decode(encodedKey) }
        .getOrElse { throw IllegalArgumentException("SOCIAL_TOKEN_ENCRYPTION_KEY must be base64 encoded") }
        .also {
            require(it.size == 32) {
                "SOCIAL_TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes"
            }
        }

    fun encrypt(value: String): String {
        require(value.isNotBlank()) { "Cannot encrypt an empty token" }
        val nonce = ByteArray(NONCE_SIZE).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return "$PREFIX${encoder.encodeToString(nonce)}:${encoder.encodeToString(ciphertext)}"
    }

    fun decrypt(value: String): String {
        require(value.startsWith(PREFIX)) { "Stored token is not encrypted" }
        val parts = value.removePrefix(PREFIX).split(':', limit = 2)
        require(parts.size == 2) { "Stored token has an invalid encrypted format" }
        val nonce = decoder.decode(parts[0])
        val ciphertext = decoder.decode(parts[1])
        require(nonce.size == NONCE_SIZE) { "Stored token has an invalid nonce" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    companion object {
        private const val PREFIX = "enc:v1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_SIZE = 12
        private const val TAG_BITS = 128
        private val secureRandom = SecureRandom()
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()

        fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)
    }
}
