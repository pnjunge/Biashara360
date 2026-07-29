package com.app.biashara.domain.repository

import com.app.biashara.domain.model.BusinessProfile
import com.app.biashara.domain.model.CyberSourceConfig
import com.app.biashara.domain.model.MpesaConfig
import com.app.biashara.data.remote.UserDto

interface BusinessRepository {
    suspend fun getProfile(): Result<BusinessProfile>
    suspend fun updateProfile(profile: BusinessProfile): Result<BusinessProfile>
    suspend fun getMpesaConfig(): Result<MpesaConfig>
    suspend fun getMpesaConfigs(): Result<List<MpesaConfig>>
    suspend fun getCyberSourceConfig(): Result<CyberSourceConfig>
    suspend fun getUsers(): Result<List<UserDto>>
    suspend fun inviteUser(name: String, email: String, phone: String, role: String): Result<UserDto>
    suspend fun toggleUserStatus(userId: String, isActive: Boolean): Result<Unit>
}
