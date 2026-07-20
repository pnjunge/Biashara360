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
        if (!loginData.requiresOtp) {
            tokenStorage.saveTokens(loginData.accessToken ?: "", loginData.refreshToken ?: "")
            loginData.user?.let { user ->
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
            }
        }
        User(
            id = loginData.userId,
            email = email,
            phone = loginData.user?.phone ?: "",
            name = loginData.user?.name ?: "",
            role = loginData.user?.let { runCatching { UserRole.valueOf(it.role) }.getOrDefault(UserRole.STAFF) } ?: UserRole.ADMIN,
            businessId = loginData.user?.businessId,
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
        val response: ApiResponse<UserDto> = client.post("$BASE_URL/auth/register") {
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
            id = response.data.id,
            email = email,
            phone = phone,
            name = name,
            role = UserRole.ADMIN,
            businessId = response.data.businessId,
            createdAt = Clock.System.now()
        )
    }

    override fun isLoggedIn(): Boolean = UserSession.isLoggedIn()

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/auth/change-password") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("currentPassword" to currentPassword, "newPassword" to newPassword))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Failed to change password" })
    }

    override suspend fun loginWithBiometric(): Result<Unit> = runCatching {
        // Option A: If a valid access token already exists, restore the session without
        // re-authenticating. This avoids sending any credentials over the network.
        val token = tokenStorage.getAccessToken()
            ?: throw Exception("No saved session. Please sign in with your password first.")
        // If a user is already set in session (in-memory), we're good
        if (UserSession.isLoggedIn()) return@runCatching
        // Otherwise try to refresh the token to rehydrate the session
        val response: ApiResponse<LoginResponse> = client.post("$BASE_URL/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refresh_token" to (tokenStorage.getRefreshToken() ?: "")))
        }.body()
        if (!response.success || response.data == null) {
            throw Exception("Session expired. Please sign in with your password.")
        }
        val loginData = response.data
        tokenStorage.saveTokens(loginData.accessToken ?: token, loginData.refreshToken ?: "")
        loginData.user?.let { user ->
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
        }
    }
}
