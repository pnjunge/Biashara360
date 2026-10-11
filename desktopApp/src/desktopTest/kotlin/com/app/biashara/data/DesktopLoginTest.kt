package com.app.biashara.data

import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.repository.AuthRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopLoginTest {
    @Test
    fun `successful login can create an OTP user on the desktop runtime`() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/v1/auth/login", request.url.encodedPath)
            respond(
                """{"success":true,"data":{"userId":"user-1","requiresOtp":true,"otpChannels":["EMAIL"]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val storage = object : TokenStorage {
            override suspend fun getAccessToken(): String? = null
            override suspend fun getRefreshToken(): String? = null
            override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
            override suspend fun clearTokens() = Unit
            override suspend fun saveSessionIdleTimeoutSeconds(seconds: Long) = Unit
            override suspend fun getSessionRemainingMillis(): Long? = null
            override suspend fun touchSession() = Unit
        }
        try {
            val user = AuthRepositoryImpl(client, storage).login("test@example.com", "test-password").getOrThrow()
            assertEquals("user-1", user.id)
            assertTrue(user.twoFactorEnabled)
            assertTrue(user.createdAt.epochSeconds > 0)
        } finally {
            client.close()
        }
    }
}
