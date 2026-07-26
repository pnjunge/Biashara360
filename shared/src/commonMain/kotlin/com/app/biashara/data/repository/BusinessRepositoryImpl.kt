package com.app.biashara.data.repository

import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.data.remote.UserDto
import com.app.biashara.domain.model.BusinessProfile
import com.app.biashara.domain.model.CyberSourceConfig
import com.app.biashara.domain.model.MpesaConfig
import com.app.biashara.domain.repository.BusinessRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class BusinessRepositoryImpl(
    private val client: HttpClient
) : BusinessRepository {

    override suspend fun getProfile(): Result<BusinessProfile> = runCatching {
        val response: ApiResponse<BusinessProfile> = client.get("$BASE_URL/business/profile").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch profile" })
        }
        response.data
    }

    override suspend fun updateProfile(profile: BusinessProfile): Result<BusinessProfile> = runCatching {
        val response: ApiResponse<BusinessProfile> = client.put("$BASE_URL/business/profile") {
            contentType(ContentType.Application.Json)
            setBody(profile)
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to update profile" })
        }
        response.data
    }

    override suspend fun getMpesaConfig(): Result<MpesaConfig> = runCatching {
        val response: ApiResponse<MpesaConfig> = client.get("$BASE_URL/settings/mpesa").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch M-Pesa config" })
        }
        response.data
    }

    override suspend fun getMpesaConfigs(): Result<List<MpesaConfig>> = runCatching {
        val response: ApiResponse<List<MpesaConfig>> =
            client.get("$BASE_URL/settings/mpesa/channels").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch M-Pesa channels" })
        }
        response.data
    }

    override suspend fun getCyberSourceConfig(): Result<CyberSourceConfig> = runCatching {
        val response: ApiResponse<CyberSourceConfig> = client.get("$BASE_URL/settings/cybersource").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch CyberSource config" })
        }
        response.data
    }

    override suspend fun getUsers(): Result<List<UserDto>> = runCatching {
        val response: ApiResponse<List<UserDto>> = client.get("$BASE_URL/users").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch users" })
        }
        response.data
    }

    override suspend fun inviteUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Result<UserDto> = runCatching {
        val response: ApiResponse<UserDto> = client.post("$BASE_URL/users") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "password" to password,
                "role" to role
            ))
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to invite user" })
        }
        response.data
    }

    override suspend fun toggleUserStatus(userId: String, isActive: Boolean): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.patch("$BASE_URL/users/$userId/status") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("isActive" to isActive))
        }.body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to update user status" })
        }
    }
}
