package com.app.biashara.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val BASE_URL = "https://api.biashara360.co.ke/v1"

fun createHttpClient(tokenStorage: TokenStorage): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenStorage.getAccessToken()
                    if (token != null) BearerTokens(token, tokenStorage.getRefreshToken() ?: "") else null
                }
                refreshTokens {
                    val refreshToken = tokenStorage.getRefreshToken() ?: return@refreshTokens null
                    try {
                        val response: RefreshTokenResponse = client.post("$BASE_URL/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refresh_token" to refreshToken))
                            markAsRefreshTokenRequest()
                        }.body()
                        tokenStorage.saveTokens(response.accessToken, response.refreshToken)
                        BearerTokens(response.accessToken, response.refreshToken)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }
}

interface TokenStorage {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
}

@kotlinx.serialization.Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

// API Response wrapper
@kotlinx.serialization.Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val errors: List<String> = emptyList()
)

// Auth DTOs
@kotlinx.serialization.Serializable
data class LoginRequest(val email: String, val password: String)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val userId: String,
    val requiresOtp: Boolean,
    val otpChannels: List<String>
)

@kotlinx.serialization.Serializable
data class OtpVerifyRequest(val userId: String, val otp: String, val channel: String)

@kotlinx.serialization.Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

@kotlinx.serialization.Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val businessId: String
)

// Mpesa Daraja DTOs
@kotlinx.serialization.Serializable
data class DarajaTokenResponse(
    val access_token: String,
    val expires_in: String
)

@kotlinx.serialization.Serializable
data class StkPushPayload(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: Int,
    val PartyA: String,
    val PartyB: String,
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)
