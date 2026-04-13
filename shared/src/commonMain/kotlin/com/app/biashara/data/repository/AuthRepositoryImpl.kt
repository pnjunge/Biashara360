package com.app.biashara.data.repository

import com.app.biashara.UserSession
import com.app.biashara.data.remote.*
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val response: ApiResponse<LoginResponse> = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Login failed" })
        }

        // Return a placeholder User with the userId so the OTP screen can use it
        val loginData = response.data
        User(
            id = loginData.userId,
            email = email,
            phone = "",
            name = "",
            role = UserRole.ADMIN,
            businessId = "",
            createdAt = Clock.System.now(),
            twoFactorEnabled = loginData.requiresOtp
        )
    }

    override suspend fun verifyOtp(
        userId: String,
        otp: String,
        channel: String
    ): Result<String> = runCatching {
        val response: ApiResponse<AuthResponse> = client.post("$BASE_URL/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(OtpVerifyRequest(userId = userId, otp = otp, channel = channel))
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "OTP verification failed" })
        }

        val authData = response.data
        tokenStorage.saveTokens(authData.accessToken, authData.refreshToken)

        // Populate UserSession with the full user
        val user = authData.user
        UserSession.setUser(
            User(
                id = user.id,
                email = user.email,
                phone = user.phone,
                name = user.name,
                role = runCatching { UserRole.valueOf(user.role) }.getOrDefault(UserRole.STAFF),
                businessId = user.businessId,
                createdAt = Clock.System.now()
            )
        )

        authData.accessToken
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        try {
            client.post("$BASE_URL/auth/logout")
        } catch (_: Exception) { /* best-effort */ }
        tokenStorage.clearTokens()
        UserSession.clearUser()
    }

    override suspend fun getCurrentUser(): User? = UserSession.currentUser.value

    override suspend fun refreshToken(): Result<String> = runCatching {
        val token = tokenStorage.getAccessToken() ?: throw Exception("No token")
        token
    }

    override suspend fun register(
        name: String,
        phone: String,
        email: String,
        password: String,
        businessName: String,
        businessType: BusinessType
    ): Result<User> = runCatching {
        val response: ApiResponse<LoginResponse> = client.post("$BASE_URL/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "password" to password,
                    "businessName" to businessName,
                    "businessType" to businessType.name
                )
            )
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Registration failed" })
        }

        User(
            id = response.data.userId,
            email = email,
            phone = phone,
            name = name,
            role = UserRole.ADMIN,
            businessId = "",
            createdAt = Clock.System.now()
        )
    }

    override fun isLoggedIn(): Boolean = UserSession.isLoggedIn()
}
