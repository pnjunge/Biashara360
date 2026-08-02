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
            refreshSessionTimeoutPolicy()
            loginData.user?.let { user -> setSessionUser(user) }
        }
        val resolvedBizId = loginData.user?.let { resolveBusinessId(it) }
        User(
            id = loginData.userId,
            email = email,
            phone = loginData.user?.phone ?: "",
            name = loginData.user?.name ?: "",
            role = loginData.user?.let { runCatching { UserRole.valueOf(it.role) }.getOrDefault(UserRole.STAFF) } ?: UserRole.ADMIN,
            businessId = resolvedBizId,
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
        refreshSessionTimeoutPolicy()

        // Populate UserSession with the full user
        setSessionUser(authData.user)

        authData.accessToken
    }

    override suspend fun resendOtp(userId: String, channel: String): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/auth/resend-otp") {
            contentType(ContentType.Application.Json)
            setBody(ResendOtpRequest(userId, channel.uppercase()))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Could not resend OTP" })
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(PasswordResetRequest(email.trim()))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Could not request password reset" })
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(PasswordResetConfirmRequest(token.trim(), newPassword))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Could not reset password" })
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
        val refreshToken = tokenStorage.getRefreshToken()
            ?: throw Exception("No saved session. Please sign in again.")
        val response: ApiResponse<AuthResponse> = client.post("$BASE_URL/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
        val authData = response.data
            ?: throw Exception(response.message.ifBlank { "Session expired. Please sign in again." })
        if (!response.success) throw Exception(response.message.ifBlank { "Session expired. Please sign in again." })
        tokenStorage.saveTokens(authData.accessToken, authData.refreshToken)
        refreshSessionTimeoutPolicy()
        setSessionUser(authData.user)
        authData.accessToken
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
        tokenStorage.getAccessToken()
            ?: throw Exception("No saved session. Please sign in with your password first.")
        if (UserSession.isLoggedIn()) return@runCatching
        refreshToken().fold(
            onSuccess = { },
            onFailure = {
                tokenStorage.clearTokens()
                throw Exception("Your saved session has expired. Please sign in again.")
            }
        )
    }

    override suspend fun loginWithPin(email: String, pin: String): Result<User> = runCatching {
        val response: ApiResponse<LoginResponse> = client.post("$BASE_URL/auth/pin-login") {
            contentType(ContentType.Application.Json)
            setBody(PinLoginRequest(email = email, pin = pin))
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "PIN login failed" })
        }

        val loginData = response.data
        if (!loginData.requiresOtp) {
            tokenStorage.saveTokens(loginData.accessToken ?: "", loginData.refreshToken ?: "")
            refreshSessionTimeoutPolicy()
            loginData.user?.let { user -> setSessionUser(user) }
        }
        val resolvedBizId = loginData.user?.let { resolveBusinessId(it) }
        User(
            id = loginData.userId,
            email = email,
            phone = loginData.user?.phone ?: "",
            name = loginData.user?.name ?: "",
            role = loginData.user?.let { runCatching { UserRole.valueOf(it.role) }.getOrDefault(UserRole.STAFF) } ?: UserRole.ADMIN,
            businessId = resolvedBizId,
            createdAt = Clock.System.now(),
            twoFactorEnabled = loginData.requiresOtp
        )
    }

    @kotlinx.serialization.Serializable
    private data class AdminBusinessItemDto(val id: String)

    private suspend fun resolveBusinessId(user: UserDto?): String? {
        if (user?.businessId?.isNotBlank() == true) return user.businessId
        return runCatching {
            val response: ApiResponse<List<AdminBusinessItemDto>> = client.get("$BASE_URL/admin/businesses").body()
            response.data?.firstOrNull()?.id
        }.getOrNull()
    }

    private suspend fun setSessionUser(user: UserDto) {
        val resolvedBusinessId = resolveBusinessId(user)
        UserSession.setUser(
            User(
                id = user.id,
                email = user.email,
                phone = user.phone,
                name = user.name,
                role = runCatching { UserRole.valueOf(user.role) }.getOrDefault(UserRole.STAFF),
                businessId = resolvedBusinessId,
                createdAt = Clock.System.now()
            )
        )
    }

    private suspend fun refreshSessionTimeoutPolicy() {
        runCatching {
            refreshSessionIdleTimeout(client)?.let { tokenStorage.saveSessionIdleTimeoutSeconds(it) }
        }
    }
}
