package com.app.biashara.data.repository

import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.SocialRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SocialRepositoryImpl(
    private val client: HttpClient
) : SocialRepository {

    override suspend fun getChannels(): Result<List<SocialChannel>> = runCatching {
        val response: ApiResponse<List<SocialChannel>> = client.get("$BASE_URL/social/channels").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch social channels" })
        }
        response.data
    }

    override suspend fun createChannel(req: SocialChannelRequest): Result<SocialChannel> = runCatching {
        val response: ApiResponse<SocialChannel> = client.post("$BASE_URL/social/channels") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to create social channel" })
        }
        response.data
    }

    override suspend fun updateChannelSettings(
        id: String,
        channelName: String,
        autoReplyEnabled: Boolean,
        aiPersonaPrompt: String
    ): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.patch("$BASE_URL/social/channels/$id/settings") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "channelName" to channelName,
                "autoReplyEnabled" to autoReplyEnabled,
                "aiPersonaPrompt" to aiPersonaPrompt
            ))
        }.body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to update channel settings" })
        }
    }

    override suspend fun getInbox(): Result<List<ConversationSummary>> = runCatching {
        val response: ApiResponse<List<ConversationSummary>> = client.get("$BASE_URL/social/inbox").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch social inbox" })
        }
        response.data
    }

    override suspend fun getConversationDetail(id: String): Result<ConversationDetailResponse> = runCatching {
        val response: ApiResponse<ConversationDetailResponse> = client.get("$BASE_URL/social/conversations/$id").body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch conversation details" })
        }
        response.data
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        messageType: String,
        mediaUrl: String?
    ): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/social/messages/send") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "conversationId" to conversationId,
                "content" to content,
                "messageType" to messageType,
                "mediaUrl" to mediaUrl
            ))
        }.body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to send message" })
        }
    }

    override suspend fun getAiReply(
        conversationId: String,
        customInstruction: String?
    ): Result<AiReplyResponse> = runCatching {
        val response: ApiResponse<AiReplyResponse> = client.post("$BASE_URL/social/messages/ai-reply") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "conversationId" to conversationId,
                "customInstruction" to customInstruction
            ))
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch AI reply suggestion" })
        }
        response.data
    }

    override suspend fun sendPaymentPrompt(
        conversationId: String,
        amount: Double,
        description: String
    ): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.post("$BASE_URL/social/payment-prompt") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "conversationId" to conversationId,
                "amount" to amount,
                "description" to description,
                "paymentMethod" to "MPESA"
            ))
        }.body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to send payment prompt" })
        }
    }
}
