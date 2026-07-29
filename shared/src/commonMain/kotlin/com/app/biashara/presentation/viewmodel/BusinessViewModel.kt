package com.app.biashara.presentation.viewmodel

import com.app.biashara.data.remote.UserDto
import com.app.biashara.domain.model.BusinessProfile
import com.app.biashara.domain.model.CyberSourceConfig
import com.app.biashara.domain.model.MpesaConfig
import com.app.biashara.domain.model.MpesaConfigRequest
import com.app.biashara.domain.repository.BusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessProfileState(
    val isLoading: Boolean = false,
    val profile: BusinessProfile? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class MpesaConfigState(
    val isLoading: Boolean = false,
    val config: MpesaConfig? = null,
    val configs: List<MpesaConfig> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class CyberSourceConfigState(
    val isLoading: Boolean = false,
    val config: CyberSourceConfig? = null,
    val error: String? = null
)

data class UsersState(
    val isLoading: Boolean = false,
    val users: List<UserDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class BusinessViewModel(
    private val repository: BusinessRepository
) : KmpViewModel() {

    private val _profileState = MutableStateFlow(BusinessProfileState())
    val profileState: StateFlow<BusinessProfileState> = _profileState.asStateFlow()

    private val _mpesaState = MutableStateFlow(MpesaConfigState())
    val mpesaState: StateFlow<MpesaConfigState> = _mpesaState.asStateFlow()

    private val _cyberSourceState = MutableStateFlow(CyberSourceConfigState())
    val cyberSourceState: StateFlow<CyberSourceConfigState> = _cyberSourceState.asStateFlow()

    private val _usersState = MutableStateFlow(UsersState())
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()

    fun loadProfile() {
        scope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }
            repository.getProfile()
                .onSuccess { profile ->
                    _profileState.update { it.copy(isLoading = false, profile = profile) }
                }
                .onFailure { exception ->
                    _profileState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun updateProfile(profile: BusinessProfile) {
        scope.launch {
            _profileState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            repository.updateProfile(profile)
                .onSuccess { updated ->
                    _profileState.update { it.copy(isSaving = false, profile = updated, saveSuccess = true) }
                }
                .onFailure { exception ->
                    _profileState.update { it.copy(isSaving = false, error = exception.message) }
                }
        }
    }

    fun loadMpesaConfig() {
        scope.launch {
            _mpesaState.update { it.copy(isLoading = true, error = null) }
            repository.getMpesaConfigs()
                .onSuccess { configs ->
                    _mpesaState.update {
                        it.copy(isLoading = false, config = configs.firstOrNull(), configs = configs)
                    }
                }
                .onFailure { exception ->
                    _mpesaState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun loadCyberSourceConfig() {
        scope.launch {
            _cyberSourceState.update { it.copy(isLoading = true, error = null) }
            repository.getCyberSourceConfig()
                .onSuccess { config ->
                    _cyberSourceState.update { it.copy(isLoading = false, config = config) }
                }
                .onFailure { exception ->
                    _cyberSourceState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    @Deprecated("Payment settings are managed in the web application")
    fun saveMpesaConfig(config: MpesaConfigRequest) {
        _mpesaState.update {
            it.copy(
                isSaving = false,
                saveSuccess = false,
                error = "M-Pesa configuration is read-only here. Manage it in the Biashara360 web application."
            )
        }
    }

    fun loadUsers() {
        scope.launch {
            _usersState.update { it.copy(isLoading = true, error = null) }
            repository.getUsers()
                .onSuccess { users ->
                    _usersState.update { it.copy(isLoading = false, users = users) }
                }
                .onFailure { exception ->
                    _usersState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun inviteUser(name: String, email: String, phone: String, role: String) {
        scope.launch {
            _usersState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            repository.inviteUser(name, email, phone, role)
                .onSuccess {
                    _usersState.update { it.copy(isSaving = false, saveSuccess = true) }
                    loadUsers()
                }
                .onFailure { exception ->
                    _usersState.update { it.copy(isSaving = false, error = exception.message) }
                }
        }
    }

    fun toggleUserStatus(userId: String, isActive: Boolean) {
        scope.launch {
            _usersState.update { it.copy(isLoading = true, error = null) }
            repository.toggleUserStatus(userId, isActive)
                .onSuccess {
                    loadUsers()
                }
                .onFailure { exception ->
                    _usersState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun clearProfileStatus() {
        _profileState.update { it.copy(saveSuccess = false, error = null) }
    }

    fun clearMpesaStatus() {
        _mpesaState.update { it.copy(saveSuccess = false, error = null) }
    }

    fun clearUsersStatus() {
        _usersState.update { it.copy(saveSuccess = false, error = null) }
    }
}
