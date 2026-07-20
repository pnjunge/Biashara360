package com.app.biashara.dto

import kotlinx.serialization.Serializable

/**
 * Request/Response DTOs for Authentication endpoints.
 * These DTOs decouple the API contract from internal database models.
 */

// ──── Registration ───────────────────────────────────────────────────────────

@Serializable
data class RegisterRequestDTO(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val businessName: String,
    val businessType: String
)

@Serializable
data class RegisterResponseDTO(
    val userId: String,
    val businessId: String,
    val message: String
)

// ──── Login ──────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequestDTO(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseDTO(
    val userId: String,
    val requiresOtp: Boolean,
    val otpChannels: List<String> = emptyList(),
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserDTO? = null
)

// ──── OTP ────────────────────────────────────────────────────────────────────

@Serializable
data class OtpVerifyRequestDTO(
    val userId: String,
    val otp: String,
    val channel: String
)

@Serializable
data class OtpResendRequestDTO(
    val userId: String,
    val channel: String = "SMS"
)

@Serializable
data class EnableOtpRequestDTO(
    val enable: Boolean,
    val channels: List<String> = listOf("SMS")
)

// ──── Token Management ───────────────────────────────────────────────────────

@Serializable
data class RefreshTokenRequestDTO(
    val refreshToken: String
)

@Serializable
data class AuthTokenResponseDTO(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 86400,  // 24 hours in seconds
    val tokenType: String = "Bearer"
)

// ──── Auth Success Response ──────────────────────────────────────────────────

@Serializable
data class AuthResponseDTO(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDTO,
    val expiresIn: Long = 86400
)

// ──── User DTO ───────────────────────────────────────────────────────────────

@Serializable
data class UserDTO(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val businessId: String?,
    val businessName: String? = null,
    val preferredLanguage: String = "en",
    val otpEnabled: Boolean = false,
    val isActive: Boolean = true
)

// ──── Password Management ────────────────────────────────────────────────────

@Serializable
data class ChangePasswordRequestDTO(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class ResetPasswordRequestDTO(
    val email: String
)

@Serializable
data class ResetPasswordConfirmDTO(
    val token: String,
    val newPassword: String
)
