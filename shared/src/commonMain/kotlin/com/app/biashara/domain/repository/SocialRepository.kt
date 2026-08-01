package com.app.biashara.domain.repository

import com.app.biashara.domain.model.*

interface SocialRepository {
    suspend fun getChannels(): Result<List<SocialChannel>>
    suspend fun createChannel(req: SocialChannelRequest): Result<SocialChannel>
    suspend fun verifyChannel(id: String): Result<Unit>
    suspend fun updateChannelSettings(
        id: String,
        channelName: String,
        autoReplyEnabled: Boolean,
        aiPersonaPrompt: String
    ): Result<Unit>
    suspend fun getInbox(): Result<List<ConversationSummary>>
    suspend fun getConversationDetail(id: String): Result<ConversationDetailResponse>
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        messageType: String = "TEXT",
        mediaUrl: String? = null
    ): Result<Unit>
    suspend fun getAiReply(
        conversationId: String,
        customInstruction: String? = null
    ): Result<AiReplyResponse>
    suspend fun sendPaymentPrompt(
        conversationId: String,
        amount: Double,
        description: String
    ): Result<Unit>
}
