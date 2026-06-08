package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── Social Channel Connections ───────────────────────────────────────────────
// One row per connected social channel per business
object SocialChannelsTable : Table("social_channels") {
    val id              = varchar("id", 36)
    val businessId      = varchar("business_id", 36).references(BusinessesTable.id)
    val platform        = varchar("platform", 20)          // WHATSAPP | INSTAGRAM | FACEBOOK | TIKTOK
    val channelName     = varchar("channel_name", 255)     // Page/account display name
    val externalId      = varchar("external_id", 255)      // WABA ID / Page ID / TikTok open_id
    val phoneNumber     = varchar("phone_number", 30).nullable()  // WhatsApp only
    val accessToken     = text("access_token")             // encrypted in prod
    val refreshToken    = text("refresh_token").nullable()
    val tokenExpiresAt  = timestamp("token_expires_at").nullable()
    val webhookVerifyToken = varchar("webhook_verify_token", 100)
    val isActive        = bool("is_active").default(true)
    val autoReplyEnabled= bool("auto_reply_enabled").default(true)
    val aiPersonaPrompt = text("ai_persona_prompt").default("")  // custom AI persona per channel
    val createdAt       = timestamp("created_at")
    val updatedAt       = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Conversations ────────────────────────────────────────────────────────────
// One conversation = one customer on one channel
object SocialConversationsTable : Table("social_conversations") {
    val id              = varchar("id", 36)
    val businessId      = varchar("business_id", 36).references(BusinessesTable.id)
    val channelId       = varchar("channel_id", 36).references(SocialChannelsTable.id)
    val platform        = varchar("platform", 20)
    val externalConvId  = varchar("external_conv_id", 255)    // Thread/conversation ID from platform
    val customerExternalId = varchar("customer_external_id", 255)  // Sender ID on the platform
    val customerName    = varchar("customer_name", 255).default("Unknown")
    val customerPhone   = varchar("customer_phone", 30).nullable()
    val customerId      = varchar("customer_id", 36).nullable()
    val status          = varchar("status", 20).default("OPEN") // OPEN | PENDING_PAYMENT | COMPLETED | CLOSED
    val assignedOrderId = varchar("assigned_order_id", 36).nullable()
    val lastMessageAt   = timestamp("last_message_at")
    val unreadCount     = integer("unread_count").default(0)
    val isAiHandled     = bool("is_ai_handled").default(false)
    val createdAt       = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(channelId, externalConvId) }
}

// ─── Messages ─────────────────────────────────────────────────────────────────
object SocialMessagesTable : Table("social_messages") {
    val id              = varchar("id", 36)
    val conversationId  = varchar("conversation_id", 36).references(SocialConversationsTable.id)
    val businessId      = varchar("business_id", 36).references(BusinessesTable.id)
    val externalMsgId   = varchar("external_msg_id", 255).nullable()  // Platform's message ID
    val direction       = varchar("direction", 10)   // INBOUND | OUTBOUND
    val senderType      = varchar("sender_type", 15) // CUSTOMER | AGENT | AI
    val content         = text("content")
    val messageType     = varchar("message_type", 20).default("TEXT")  // TEXT | IMAGE | AUDIO | ORDER | PAYMENT_REQUEST | TEMPLATE
    val mediaUrl        = text("media_url").nullable()
    val status          = varchar("status", 20).default("SENT")  // SENT | DELIVERED | READ | FAILED
    val isAiGenerated   = bool("is_ai_generated").default(false)
    val metadata        = text("metadata").nullable()  // JSON: order snapshot, payment link etc.
    val createdAt       = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Social Orders ────────────────────────────────────────────────────────────
// Links a social conversation to a B360 order
object SocialOrdersTable : Table("social_orders") {
    val id              = varchar("id", 36)
    val businessId      = varchar("business_id", 36).references(BusinessesTable.id)
    val conversationId  = varchar("conversation_id", 36).references(SocialConversationsTable.id)
    val orderId         = varchar("order_id", 36).references(OrdersTable.id)
    val platform        = varchar("platform", 20)
    val paymentLinkSent = bool("payment_link_sent").default(false)
    val paymentLinkSentAt = timestamp("payment_link_sent_at").nullable()
    val createdAt       = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
