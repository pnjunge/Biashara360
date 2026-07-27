package com.app.biashara

import com.app.biashara.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped singleton that holds the currently authenticated user.
 * Populated by AuthRepositoryImpl after a successful login/OTP flow.
 */
object UserSession {
    const val DEFAULT_BUSINESS_ID = "40ef1d93-6112-424f-821f-3c3c818c01ee"

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setUser(user: User) {
        val effectiveBusinessId = user.businessId?.ifBlank { null } ?: DEFAULT_BUSINESS_ID
        _currentUser.value = user.copy(businessId = effectiveBusinessId)
    }

    fun clearUser() {
        _currentUser.value = null
    }

    fun getBusinessId(): String {
        val id = _currentUser.value?.businessId
        return if (!id.isNullOrBlank()) id else DEFAULT_BUSINESS_ID
    }

    fun getUserName(): String = _currentUser.value?.name ?: ""
    fun getEmail(): String = _currentUser.value?.email ?: ""
    fun isLoggedIn(): Boolean = _currentUser.value != null
}
