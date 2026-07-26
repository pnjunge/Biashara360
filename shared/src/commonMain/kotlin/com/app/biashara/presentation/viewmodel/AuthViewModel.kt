package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.BusinessType
import com.app.biashara.domain.usecase.*
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
    val otpCooldownSeconds: Int = 0,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null,
    val isChangingPassword: Boolean = false,
    val passwordResetRequested: Boolean = false,
    val passwordResetCompleted: Boolean = false
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val loginWithBiometricUseCase: LoginWithBiometricUseCase,
    private val resendOtpUseCase: ResendOtpUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase
) : KmpViewModel() {

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
                    if (user.twoFactorEnabled) {
                        _state.update {
                            it.copy(isLoading = false, step = AuthStep.Otp(userId = user.id))
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun loginWithBiometric(onSuccess: () -> Unit) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginWithBiometricUseCase().fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                    onSuccess()
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
                        it.copy(isLoading = false, step = AuthStep.Otp(userId = user.id))
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        scope.launch {
            _state.update { it.copy(isChangingPassword = true, passwordChangeError = null, passwordChangeSuccess = false) }
            changePasswordUseCase(currentPassword, newPassword).fold(
                onSuccess = {
                    _state.update { it.copy(isChangingPassword = false, passwordChangeSuccess = true) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isChangingPassword = false, passwordChangeError = e.message) }
                }
            )
        }
    }

    fun dismissPasswordChangeResult() {
        _state.update { it.copy(passwordChangeSuccess = false, passwordChangeError = null) }
    }

    fun startOtpCooldown() {
        scope.launch {
            for (seconds in 60 downTo 0) {
                _state.update { it.copy(otpCooldownSeconds = seconds) }
                if (seconds > 0) kotlinx.coroutines.delay(1_000)
            }
        }
    }

    fun resendOtp(channel: String) {
        if (state.value.otpCooldownSeconds > 0) return
        if (state.value.step !is AuthStep.Otp) return
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val step = state.value.step as AuthStep.Otp
            resendOtpUseCase(step.userId, channel.uppercase()).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    startOtpCooldown()
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Email is required") }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            requestPasswordResetUseCase(email).fold(
                onSuccess = { _state.update { it.copy(isLoading = false, passwordResetRequested = true) } },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun confirmPasswordReset(token: String, newPassword: String) {
        if (token.length != 6 || newPassword.length < 8) {
            _state.update { it.copy(error = "Enter the 6-digit code and a password of at least 8 characters") }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            confirmPasswordResetUseCase(token, newPassword).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isLoading = false, passwordResetCompleted = true, passwordResetRequested = false)
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun clearPasswordResetState() {
        _state.update {
            it.copy(passwordResetRequested = false, passwordResetCompleted = false, error = null)
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

    fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }
}
