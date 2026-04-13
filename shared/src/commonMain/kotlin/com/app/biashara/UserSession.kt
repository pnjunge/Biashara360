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
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setUser(user: User) {
        _currentUser.value = user
    }

    fun clearUser() {
        _currentUser.value = null
    }

    fun getBusinessId(): String = _currentUser.value?.businessId ?: ""
    fun getUserName(): String = _currentUser.value?.name ?: ""
    fun getEmail(): String = _currentUser.value?.email ?: ""
    fun isLoggedIn(): Boolean = _currentUser.value != null
}
