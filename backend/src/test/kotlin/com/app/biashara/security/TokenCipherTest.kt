package com.app.biashara.security

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TokenCipherTest {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    @Test
    fun `encrypts with a unique nonce and decrypts`() {
        val cipher = TokenCipher(key)
        val first = cipher.encrypt("merchant-access-token")
        val second = cipher.encrypt("merchant-access-token")

        assertTrue(TokenCipher.isEncrypted(first))
        assertNotEquals(first, second)
        assertEquals("merchant-access-token", cipher.decrypt(first))
        assertEquals("merchant-access-token", cipher.decrypt(second))
    }

    @Test
    fun `rejects invalid key size and plaintext values`() {
        assertFailsWith<IllegalArgumentException> {
            TokenCipher(Base64.getEncoder().encodeToString(ByteArray(16)))
        }
        assertFailsWith<IllegalArgumentException> {
            TokenCipher(key).decrypt("plaintext-token")
        }
    }
}
