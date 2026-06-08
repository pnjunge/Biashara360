package com.app.biashara.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ─── Channel Connection ───────────────────────────────────────────────────────

@Serializable
data class SocialChannelRequest(
    val platform: String,           // WHATSAPP | INSTAGRAM | FACEBOOK | TIKTOK
    val channelName: String,
    val externalId: String,         // WABA ID, Page ID, TikTok open_id
    val phoneNumber: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val autoReplyEnabled: Boolean = true,
    val aiPersonaPrompt: String = ""
)

@Serializable
data class SocialChannelResponse(
    val id: String,
    val platform: String,
    val channelName: String,
    val externalId: String,
    val phoneNumber: String?,
    val isActive: Boolean,
    val autoReplyEnabled: Boolean,
    val aiPersonaPrompt: String,
    val webhookVerifyToken: String,  // used to verify webhook with Meta/TikTok
    val webhookUrl: String,          // URL to register on Meta Developer console
    val unreadCount: Int,
    val createdAt: String
)

// ─── Conversations ────────────────────────────────────────────────────────────

@Serializable
data class ConversationResponse(
    val id: String,
    val platform: String,
    val channelName: String,
    val customerName: String,
    val customerPhone: String?,
    val customerId: String?,
    val status: String,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int,
    val isAiHandled: Boolean,
    val assignedOrderId: String?,
    val platformAvatarUrl: String?
)

@Serializable
data class ConversationDetailResponse(
    val conversation: ConversationResponse,
    val messages: List<MessageResponse>,
    val suggestedReplies: List<String>,
    val detectedProducts: List<ProductMention>
)

@Serializable
data class ProductMention(
    val productId: String?,
    val productName: String,
    val confidence: Double,
    val quantity: Int?
)

// ─── Messages ─────────────────────────────────────────────────────────────────

@Serializable
data class MessageResponse(
    val id: String,
    val direction: String,          // INBOUND | OUTBOUND
    val senderType: String,         // CUSTOMER | AGENT | AI
    val content: String,
    val messageType: String,
    val mediaUrl: String?,
    val status: String,
    val isAiGenerated: Boolean,
    val createdAt: String
)

@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val messageType: String = "TEXT",
    val mediaUrl: String? = null
)

// ─── AI Reply ─────────────────────────────────────────────────────────────────

@Serializable
data class AiReplyRequest(
    val conversationId: String,
    val customInstruction: String? = null  // override AI persona for this reply
)

@Serializable
data class AiReplyResponse(
    val suggestedReply: String,
    val detectedIntent: String,     // INQUIRY | ORDER | COMPLAINT | GREETING | PAYMENT_FOLLOWUP | OTHER
    val detectedProducts: List<ProductMention>,
    val shouldCreateOrder: Boolean,
    val paymentPromptSuggested: Boolean,
    val confidence: Double
)

// ─── Payment Prompt ───────────────────────────────────────────────────────────

@Serializable
data class SendPaymentPromptRequest(
    val conversationId: String,
    val orderId: String? = null,        // existing order, or null to create ad-hoc
    val amount: Double,
    val description: String,
    val paymentMethod: String = "MPESA" // MPESA | CARD | BOTH
)

@Serializable
data class PaymentPromptResponse(
    val messageId: String,
    val paymentMessage: String,         // The message sent to the customer
    val mpesaPaybillMessage: String?,   // e.g. "Lipa kwa Mpesa: 174379, Account: ORD-001"
    val stkPushInitiated: Boolean,
    val checkoutRequestId: String?
)

// ─── Order from Social ────────────────────────────────────────────────────────

@Serializable
data class CreateSocialOrderRequest(
    val conversationId: String,
    val items: List<OrderItemRequest>,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String = "",
    val paymentMethod: String = "MPESA",
    val sendPaymentPromptImmediately: Boolean = true
)

// ─── Webhook Payloads (inbound from Meta / TikTok) ───────────────────────────

// Meta (WhatsApp, Instagram, Facebook) — shared webhook structure
@Serializable
data class MetaWebhookPayload(
    val `object`: String,
    val entry: List<MetaEntry>
)

@Serializable
data class MetaEntry(
    val id: String,
    val changes: List<MetaChange>? = null,
    val messaging: List<MetaMessaging>? = null  // Messenger uses this
)

@Serializable
data class MetaChange(
    val value: MetaChangeValue,
    val field: String
)

@Serializable
data class MetaChangeValue(
    val messaging_product: String? = null,
    val metadata: MetaMetadata? = null,
    val messages: List<MetaMessage>? = null,
    val statuses: List<MetaStatus>? = null,
    val contacts: List<MetaContact>? = null,
    val from: String? = null,         // Instagram comment sender
    val text: String? = null,         // Instagram comment text
    val id: String? = null,
    val media: MetaMedia? = null
)

@Serializable
data class MetaMetadata(val display_phone_number: String, val phone_number_id: String)

@Serializable
data class MetaMessage(
    val id: String,
    val from: String,
    val timestamp: String,
    val type: String,
    val text: MetaText? = null,
    val image: MetaMedia? = null,
    val audio: MetaMedia? = null
)

@Serializable
data class MetaText(val body: String)
@Serializable
data class MetaMedia(val id: String? = null, val link: String? = null, val mime_type: String? = null)
@Serializable
data class MetaStatus(val id: String, val recipient_id: String, val status: String, val timestamp: String)
@Serializable
data class MetaContact(val profile: MetaProfile, val wa_id: String)
@Serializable
data class MetaProfile(val name: String)
@Serializable
data class MetaMessaging(val sender: MetaSender, val recipient: MetaSender, val timestamp: Long, val message: MetaMessengerMessage? = null)
@Serializable
data class MetaSender(val id: String)
@Serializable
data class MetaMessengerMessage(val mid: String, val text: String? = null)

// TikTok webhook
@Serializable
data class TikTokWebhookPayload(
    val event: String,          // direct_message, comment
    val data: TikTokEventData
)

@Serializable
data class TikTokEventData(
    val message_id: String? = null,
    val from_user_id: String,
    val to_user_id: String? = null,
    val content: String,
    val timestamp: Long,
    val comment_id: String? = null,
    val video_id: String? = null
)

// ─── Stats ────────────────────────────────────────────────────────────────────

@Serializable
data class SocialInboxStats(
    val totalConversations: Int,
    val openConversations: Int,
    val pendingPayment: Int,
    val aiHandled: Int,
    val ordersFromSocial: Int,
    val revenueFromSocial: Double,
    val byPlatform: List<PlatformStat>
)

@Serializable
data class PlatformStat(
    val platform: String,
    val conversations: Int,
    val orders: Int,
    val revenue: Double,
    val isConnected: Boolean
)
