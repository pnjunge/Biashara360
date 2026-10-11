package com.app.biashara.data

import com.app.biashara.UserSession
import com.app.biashara.data.remote.*
import com.app.biashara.data.repository.AuthRepositoryImpl
import io.ktor.client.call.body
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class AccountSwitchTest {
    @Test
    fun `account switch replaces cached bearer token and logout clears it`() = runBlocking {
        var accessToken: String? = "previous-account"
        var refreshToken: String? = "previous-refresh"
        val storage = object : TokenStorage {
            override suspend fun getAccessToken() = accessToken
            override suspend fun getRefreshToken() = refreshToken
            override suspend fun saveTokens(accessToken: String, refreshToken: String) {
                store(accessToken, refreshToken)
            }
            private fun store(access: String, refresh: String) { accessToken = access; refreshToken = refresh }
            override suspend fun clearTokens() { accessToken = null; refreshToken = null }
            override suspend fun saveSessionIdleTimeoutSeconds(seconds: Long) = Unit
            override suspend fun getSessionRemainingMillis(): Long? = null
            override suspend fun touchSession() = Unit
        }
        val client = createHttpClient(storage, networkEngine = MockEngine { request ->
            val payload = when (request.url.encodedPath) {
                "/v1/auth/login" -> """{"success":true,"data":{"userId":"hotel-staff","requiresOtp":false,"otpChannels":[],"accessToken":"hotel-account","refreshToken":"hotel-refresh","user":{"id":"hotel-staff","name":"Hotel Staff","email":"hotel@example.test","phone":"","role":"STAFF","businessId":"merchant"}}}"""
                "/v1/access/me" -> """{"success":true,"data":"${request.headers[HttpHeaders.Authorization] ?: "anonymous"}"}"""
                else -> """{"success":true}"""
            }
            respond(payload, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            suspend fun identity(): String? = client.get("$BASE_URL/access/me").body<ApiResponse<String>>().data
            assertEquals("Bearer previous-account", identity())
            AuthRepositoryImpl(client, storage).login("hotel@example.test", "synthetic-password").getOrThrow()
            assertEquals("Bearer hotel-account", identity())
            AuthRepositoryImpl(client, storage).logout().getOrThrow()
            assertEquals("anonymous", identity())
        } finally {
            client.close()
            UserSession.clearUser()
        }
    }
}
