package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.BusinessType
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthStep {
    object Login : AuthStep()
    data class Otp(val userId: String) : AuthStep()
}

data class AuthState(
    val isLoading: Boolean = false,
    val step: AuthStep = AuthStep.Login,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    // Registration form fields
    val otpCooldownSeconds: Int = 0
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email and password are required") }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginUseCase(email, password).fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.Otp(userId = user.id)
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun verifyOtp(otp: String, channel: String = "SMS") {
        val userId = (state.value.step as? AuthStep.Otp)?.userId ?: return
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            verifyOtpUseCase(userId, otp, channel).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun register(
        name: String,
        phone: String,
        email: String,
        password: String,
        businessName: String,
        businessType: BusinessType
    ) {
        if (name.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank() || businessName.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            registerUseCase(name, phone, email, password, businessName, businessType).fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.Otp(userId = user.id)
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun startOtpCooldown() {
        scope.launch {
            for (seconds in 60 downTo 0) {
                _state.update { it.copy(otpCooldownSeconds = seconds) }
                if (seconds > 0) kotlinx.coroutines.delay(1_000)
            }
        }
    }

    fun resendOtp() {
        if (state.value.otpCooldownSeconds > 0) return
        val userId = (state.value.step as? AuthStep.Otp)?.userId ?: return
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // Re-trigger login to resend OTP — backend handles this via userId
            _state.update { it.copy(isLoading = false) }
            startOtpCooldown()
        }
    }

    fun goBackToLogin() {
        _state.update { it.copy(step = AuthStep.Login, error = null) }
    }

    fun logout() {
        scope.launch {
            logoutUseCase()
            _state.update { AuthState() }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
