package com.app.biashara.presentation.viewmodel

import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.SocialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Core Data
    val channels: List<SocialChannel> = emptyList(),
    val conversations: List<ConversationSummary> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<SocialMessage> = emptyList(),
    val suggestedReplies: List<String> = emptyList(),
    val aiLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    
    // Onboarding Wizard State
    val onboardingStep: Int = 0,
    val selectedPlatform: String? = null,
    val channelNameInput: String = "",
    val externalIdInput: String = "",
    val phoneNumberInput: String = "",
    val accessTokenInput: String = "",
    val autoReplyEnabledInput: Boolean = true,
    val aiPersonaPromptInput: String = "Hujambo! I am the automated customer assistant for our business. I speak a mix of English and Swahili (Sheng). I am here to help you browse products, check pricing, and complete your purchase. Be friendly and keep responses short!",
    
    val savingChannel: Boolean = false,
    val createdChannel: SocialChannel? = null,
    val verificationStage: String = "idle", // idle, testing, success, failed
    val verificationLogs: List<String> = emptyList(),
    val verifyProgress: Float = 0.0f
)

class SocialViewModel(
    private val socialRepository: SocialRepository
) : KmpViewModel() {

    private val _state = MutableStateFlow(SocialState())
    val state: StateFlow<SocialState> = _state.asStateFlow()

    fun loadChannelsAndInbox() {
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            socialRepository.getChannels()
                .onSuccess { channelsList ->
                    _state.update { it.copy(channels = channelsList) }
                    if (channelsList.isNotEmpty()) {
                        loadInbox()
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun loadInbox() {
        scope.launch {
            socialRepository.getInbox()
                .onSuccess { inbox ->
                    _state.update { 
                        it.copy(
                            conversations = inbox,
                            isLoading = false
                        )
                    }
                    if (inbox.isNotEmpty() && _state.value.activeConversationId == null) {
                        selectConversation(inbox.first().id)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun selectConversation(id: String) {
        _state.update { it.copy(activeConversationId = id, messages = emptyList(), suggestedReplies = emptyList()) }
        scope.launch {
            socialRepository.getConversationDetail(id)
                .onSuccess { detail ->
                    if (_state.value.activeConversationId == id) {
                        _state.update {
                            it.copy(
                                messages = detail.messages,
                                suggestedReplies = detail.suggestedReplies
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
        }
    }

    fun selectPlatform(platform: String) {
        _state.update {
            it.copy(
                selectedPlatform = platform,
                channelNameInput = "${platform.lowercase().replaceFirstChar { c -> c.uppercase() }} Integration",
                onboardingStep = 1,
                error = null
            )
        }
    }

    fun updateNameInput(value: String) = _state.update { it.copy(channelNameInput = value) }
    fun updateExternalIdInput(value: String) = _state.update { it.copy(externalIdInput = value) }
    fun updatePhoneNumberInput(value: String) = _state.update { it.copy(phoneNumberInput = value) }
    fun updateAccessTokenInput(value: String) = _state.update { it.copy(accessTokenInput = value) }
    fun updateAutoReplyInput(value: Boolean) = _state.update { it.copy(autoReplyEnabledInput = value) }
    fun updatePersonaPromptInput(value: String) = _state.update { it.copy(aiPersonaPromptInput = value) }

    fun nextStep() {
        val next = _state.value.onboardingStep + 1
        _state.update { it.copy(onboardingStep = next, error = null) }
    }

    fun prevStep() {
        val prev = (_state.value.onboardingStep - 1).coerceAtLeast(0)
        _state.update { it.copy(onboardingStep = prev, error = null) }
    }

    fun saveCredentials() {
        val stateVal = _state.value
        if (stateVal.selectedPlatform == "WHATSAPP") {
            _state.update { it.copy(error = "Complete WhatsApp Embedded Signup in the Biashara360 web app. Merchant tokens are not accepted.") }
            return
        }
        if (stateVal.channelNameInput.isBlank()) {
            _state.update { it.copy(error = "Please specify a display name for this channel.") }
            return
        }
        if (stateVal.externalIdInput.isBlank()) {
            _state.update { it.copy(error = "External ID is required.") }
            return
        }
        if (stateVal.accessTokenInput.isBlank()) {
            _state.update { it.copy(error = "Developer Access Token is required.") }
            return
        }

        _state.update { it.copy(savingChannel = true, error = null) }
        scope.launch {
            val req = SocialChannelRequest(
                platform = stateVal.selectedPlatform ?: "WHATSAPP",
                channelName = stateVal.channelNameInput,
                externalId = stateVal.externalIdInput,
                phoneNumber = if (stateVal.selectedPlatform == "WHATSAPP") stateVal.phoneNumberInput else null,
                accessToken = stateVal.accessTokenInput,
                autoReplyEnabled = false,
                aiPersonaPrompt = ""
            )
            socialRepository.createChannel(req)
                .onSuccess { channel ->
                    _state.update {
                        it.copy(
                            createdChannel = channel,
                            savingChannel = false,
                            onboardingStep = 2
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(savingChannel = false, error = error.message) }
                }
        }
    }

    fun runVerificationScan() {
        val created = _state.value.createdChannel ?: return
        _state.update {
            it.copy(
                verificationStage = "testing",
                verifyProgress = 0.0f,
                verificationLogs = emptyList()
            )
        }

        scope.launch {
            _state.update {
                it.copy(verificationLogs = listOf("Verifying channel with the platform API…"), verifyProgress = 0.5f)
            }
            socialRepository.verifyChannel(created.id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            verificationStage = "success",
                            verifyProgress = 1.0f,
                            verificationLogs = it.verificationLogs + "Channel connection verified."
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            verificationStage = "failed",
                            verifyProgress = 0.0f,
                            verificationLogs = it.verificationLogs + "Verification failed.",
                            error = error.message
                        )
                    }
                }
        }
    }

    fun saveAiPersona() {
        val created = _state.value.createdChannel ?: return
        _state.update { it.copy(savingChannel = true, error = null) }
        scope.launch {
            socialRepository.updateChannelSettings(
                id = created.id,
                channelName = created.channelName,
                autoReplyEnabled = _state.value.autoReplyEnabledInput,
                aiPersonaPrompt = _state.value.aiPersonaPromptInput
            ).onSuccess {
                _state.update {
                    it.copy(
                        savingChannel = false,
                        onboardingStep = 0,
                        selectedPlatform = null,
                        createdChannel = null,
                        verificationStage = "idle"
                    )
                }
                loadChannelsAndInbox()
            }.onFailure { error ->
                _state.update { it.copy(savingChannel = false, error = error.message) }
            }
        }
    }

    fun handleSendMessage(content: String, messageType: String = "TEXT", mediaUrl: String? = null) {
        val convId = _state.value.activeConversationId ?: return
        if (content.isBlank()) return
        _state.update { it.copy(isSendingMessage = true) }
        scope.launch {
            socialRepository.sendMessage(convId, content, messageType, mediaUrl)
                .onSuccess {
                    _state.update { it.copy(isSendingMessage = false) }
                    selectConversation(convId)
                }
                .onFailure { error ->
                    _state.update { it.copy(isSendingMessage = false, error = error.message) }
                }
        }
    }

    fun generateAiSuggestion() {
        val convId = _state.value.activeConversationId ?: return
        _state.update { it.copy(aiLoading = true) }
        scope.launch {
            socialRepository.getAiReply(convId, null)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            aiLoading = false,
                            suggestedReplies = listOf(response.suggestedReply)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(aiLoading = false, error = error.message) }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun resetWizard() {
        _state.update {
            it.copy(
                onboardingStep = 0,
                selectedPlatform = null,
                createdChannel = null,
                verificationStage = "idle",
                channelNameInput = "",
                externalIdInput = "",
                phoneNumberInput = "",
                accessTokenInput = "",
                error = null
            )
        }
    }

    fun handleSendPaymentPrompt(amount: Double, description: String) {
        val convId = _state.value.activeConversationId ?: return
        if (amount <= 0 || description.isBlank()) return
        _state.update { it.copy(isSendingMessage = true) }
        scope.launch {
            socialRepository.sendPaymentPrompt(convId, amount, description)
                .onSuccess {
                    _state.update { it.copy(isSendingMessage = false) }
                    selectConversation(convId)
                }
                .onFailure { error ->
                    _state.update { it.copy(isSendingMessage = false, error = error.message) }
                }
        }
    }
}
