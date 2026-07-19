package com.app.biashara.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SocialChannel(
    val id: String,
    val platform: String,
    val channelName: String,
    val externalId: String,
    val phoneNumber: String? = null,
    val isActive: Boolean = true,
    val autoReplyEnabled: Boolean = true,
    val aiPersonaPrompt: String = "",
    val webhookVerifyToken: String = "",
    val webhookUrl: String = "",
    val unreadCount: Int = 0,
    val createdAt: String = ""
)

@Serializable
data class SocialChannelRequest(
    val platform: String,
    val channelName: String,
    val externalId: String,
    val phoneNumber: String? = null,
    val accessToken: String,
    val autoReplyEnabled: Boolean = true,
    val aiPersonaPrompt: String = ""
)

@Serializable
data class ConversationSummary(
    val id: String,
    val platform: String,
    val channelName: String,
    val customerName: String,
    val customerPhone: String? = null,
    val customerId: String? = null,
    val status: String = "OPEN",
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val unreadCount: Int = 0,
    val isAiHandled: Boolean = false,
    val assignedOrderId: String? = null,
    val platformAvatarUrl: String? = null
)

@Serializable
data class SocialMessage(
    val id: String,
    val direction: String,          // INBOUND | OUTBOUND
    val senderType: String,         // CUSTOMER | AGENT | AI
    val content: String,
    val messageType: String = "TEXT",
    val mediaUrl: String? = null,
    val status: String = "SENT",
    val isAiGenerated: Boolean = false,
    val createdAt: String = ""
)

@Serializable
data class ConversationDetailResponse(
    val conversation: ConversationSummary,
    val messages: List<SocialMessage> = emptyList(),
    val suggestedReplies: List<String> = emptyList()
)

@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val messageType: String = "TEXT",
    val mediaUrl: String? = null
)

@Serializable
data class AiReplyRequest(
    val conversationId: String,
    val customInstruction: String? = null
)

@Serializable
data class AiReplyResponse(
    val suggestedReply: String,
    val detectedIntent: String = "OTHER",
    val shouldCreateOrder: Boolean = false,
    val paymentPromptSuggested: Boolean = false,
    val confidence: Double = 1.0
)
