package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Social Commerce Service
//
// Handles: channel management, webhook processing, AI replies (Claude API),
// payment prompts (Mpesa STK), order creation from social conversations
//
// Platforms:
//   WhatsApp  — Meta Cloud API v20.0
//   Instagram — Meta Graph API v20.0 (DMs + comment replies)
//   Facebook  — Meta Graph API v20.0 (Messenger + post comments)
//   TikTok    — TikTok for Business Direct Message API v2
// ─────────────────────────────────────────────────────────────────────────────

class SocialService(
    private val httpClient: HttpClient,
    private val mpesaService: MpesaService,
    private val orderService: OrderService,
    private val productService: ProductService
) {

    // ── Channel Management ────────────────────────────────────────────────────

    fun getChannels(businessId: String): List<SocialChannelResponse> = transaction {
        SocialChannelsTable.select { SocialChannelsTable.businessId eq businessId }
            .map { it.toChannelResponse(businessId) }
    }

    fun connectChannel(businessId: String, req: SocialChannelRequest): ApiResponse<SocialChannelResponse> = transaction {
        val id           = generateId()
        val verifyToken  = UUID.randomUUID().toString().replace("-", "")
        val now          = Clock.System.now()
        SocialChannelsTable.insert {
            it[SocialChannelsTable.id]           = id
            it[SocialChannelsTable.businessId]   = businessId
            it[platform]                         = req.platform.uppercase()
            it[channelName]                      = req.channelName
            it[externalId]                       = req.externalId
            it[phoneNumber]                      = req.phoneNumber
            it[accessToken]                      = req.accessToken
            it[refreshToken]                     = req.refreshToken
            it[autoReplyEnabled]                 = req.autoReplyEnabled
            it[aiPersonaPrompt]                  = req.aiPersonaPrompt
            it[webhookVerifyToken]               = verifyToken
            it[createdAt]                        = now
            it[updatedAt]                        = now
        }
        val channel = SocialChannelsTable.select { SocialChannelsTable.id eq id }.first()
        ApiResponse(true, data = channel.toChannelResponse(businessId), message = "${req.platform} channel connected")
    }

    fun disconnectChannel(businessId: String, channelId: String): ApiResponse<Unit> = transaction {
        val updated = SocialChannelsTable.update({
            (SocialChannelsTable.id eq channelId) and (SocialChannelsTable.businessId eq businessId)
        }) { it[isActive] = false }
        if (updated == 0) ApiResponse(false, message = "Channel not found")
        else ApiResponse(true, message = "Channel disconnected")
    }

    // ── Conversation List ─────────────────────────────────────────────────────

    fun getConversations(
        businessId: String,
        platform: String? = null,
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 30
    ): PagedResponse<ConversationResponse> = transaction {
        var query = SocialConversationsTable.select { SocialConversationsTable.businessId eq businessId }
        if (!platform.isNullOrBlank()) query = query.andWhere { SocialConversationsTable.platform eq platform.uppercase() }
        if (!status.isNullOrBlank()) query = query.andWhere { SocialConversationsTable.status eq status.uppercase() }
        val total = query.count().toInt()
        val convs = query
            .orderBy(SocialConversationsTable.lastMessageAt, SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { row ->
                val lastMsg = SocialMessagesTable.select { SocialMessagesTable.conversationId eq row[SocialConversationsTable.id] }
                    .orderBy(SocialMessagesTable.createdAt, SortOrder.DESC).firstOrNull()
                val channelName = SocialChannelsTable.select { SocialChannelsTable.id eq row[SocialConversationsTable.channelId] }
                    .firstOrNull()?.get(SocialChannelsTable.channelName) ?: ""
                ConversationResponse(
                    id                = row[SocialConversationsTable.id],
                    platform          = row[SocialConversationsTable.platform],
                    channelName       = channelName,
                    customerName      = row[SocialConversationsTable.customerName],
                    customerPhone     = row[SocialConversationsTable.customerPhone],
                    customerId        = row[SocialConversationsTable.customerId],
                    status            = row[SocialConversationsTable.status],
                    lastMessage       = lastMsg?.get(SocialMessagesTable.content)?.take(80) ?: "",
                    lastMessageAt     = row[SocialConversationsTable.lastMessageAt].toString(),
                    unreadCount       = row[SocialConversationsTable.unreadCount],
                    isAiHandled       = row[SocialConversationsTable.isAiHandled],
                    assignedOrderId   = row[SocialConversationsTable.assignedOrderId],
                    platformAvatarUrl = null
                )
            }
        PagedResponse(convs, total, page, pageSize, (page * pageSize) < total)
    }

    // ── Conversation Detail ───────────────────────────────────────────────────

    fun getConversationDetail(businessId: String, conversationId: String): ConversationDetailResponse? = transaction {
        val convRow = SocialConversationsTable.select {
            (SocialConversationsTable.id eq conversationId) and
            (SocialConversationsTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction null

        val channelName = SocialChannelsTable.select { SocialChannelsTable.id eq convRow[SocialConversationsTable.channelId] }
            .firstOrNull()?.get(SocialChannelsTable.channelName) ?: ""

        val messages = SocialMessagesTable.select { SocialMessagesTable.conversationId eq conversationId }
            .orderBy(SocialMessagesTable.createdAt, SortOrder.ASC)
            .map { MessageResponse(
                id           = it[SocialMessagesTable.id],
                direction    = it[SocialMessagesTable.direction],
                senderType   = it[SocialMessagesTable.senderType],
                content      = it[SocialMessagesTable.content],
                messageType  = it[SocialMessagesTable.messageType],
                mediaUrl     = it[SocialMessagesTable.mediaUrl],
                status       = it[SocialMessagesTable.status],
                isAiGenerated = it[SocialMessagesTable.isAiGenerated],
                createdAt    = it[SocialMessagesTable.createdAt].toString()
            )}

        // Mark as read
        SocialConversationsTable.update({ SocialConversationsTable.id eq conversationId }) {
            it[unreadCount] = 0
        }

        ConversationDetailResponse(
            conversation     = ConversationResponse(
                id            = convRow[SocialConversationsTable.id],
                platform      = convRow[SocialConversationsTable.platform],
                channelName   = channelName,
                customerName  = convRow[SocialConversationsTable.customerName],
                customerPhone = convRow[SocialConversationsTable.customerPhone],
                customerId    = convRow[SocialConversationsTable.customerId],
                status        = convRow[SocialConversationsTable.status],
                lastMessage   = messages.lastOrNull()?.content?.take(80) ?: "",
                lastMessageAt = convRow[SocialConversationsTable.lastMessageAt].toString(),
                unreadCount   = 0,
                isAiHandled   = convRow[SocialConversationsTable.isAiHandled],
                assignedOrderId = convRow[SocialConversationsTable.assignedOrderId],
                platformAvatarUrl = null
            ),
            messages         = messages,
            suggestedReplies = listOf("Asante! Nitapeleka order yako sasa.", "Tafadhali tuma namba yako ya simu.", "Bei ni KES…"),
            detectedProducts = emptyList()
        )
    }

    // ── Send Message ──────────────────────────────────────────────────────────

    suspend fun sendMessage(businessId: String, req: SendMessageRequest): ApiResponse<MessageResponse> {
        val conv = transaction {
            SocialConversationsTable.select {
                (SocialConversationsTable.id eq req.conversationId) and
                (SocialConversationsTable.businessId eq businessId)
            }.firstOrNull()
        } ?: return ApiResponse(false, message = "Conversation not found")

        val channel = transaction {
            SocialChannelsTable.select { SocialChannelsTable.id eq conv[SocialConversationsTable.channelId] }.firstOrNull()
        } ?: return ApiResponse(false, message = "Channel not found")

        val platform = conv[SocialConversationsTable.platform]
        val recipientId = conv[SocialConversationsTable.customerExternalId]

        // Send via appropriate platform API
        val sendResult = when (platform) {
            "WHATSAPP"  -> sendWhatsAppMessage(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], recipientId, req.content)
            "INSTAGRAM" -> sendInstagramDm(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], recipientId, req.content)
            "FACEBOOK"  -> sendFacebookMessage(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], recipientId, req.content)
            "TIKTOK"    -> sendTikTokDm(channel[SocialChannelsTable.accessToken], conv[SocialConversationsTable.externalConvId], req.content)
            else        -> false
        }

        val now = Clock.System.now()
        val msgId = generateId()
        transaction {
            SocialMessagesTable.insert {
                it[id]             = msgId
                it[conversationId] = req.conversationId
                it[SocialMessagesTable.businessId] = businessId
                it[direction]      = "OUTBOUND"
                it[senderType]     = "AGENT"
                it[content]        = req.content
                it[messageType]    = req.messageType
                it[mediaUrl]       = req.mediaUrl
                it[status]         = if (sendResult) "SENT" else "FAILED"
                it[createdAt]      = now
            }
            SocialConversationsTable.update({ SocialConversationsTable.id eq req.conversationId }) {
                it[lastMessageAt] = now
            }
        }
        val msg = MessageResponse(msgId, "OUTBOUND", "AGENT", req.content, req.messageType, req.mediaUrl, if (sendResult) "SENT" else "FAILED", false, now.toString())
        return ApiResponse(sendResult, data = msg, message = if (!sendResult) "Message saved but platform delivery failed" else "")
    }

    // ── AI Reply (Claude API) ─────────────────────────────────────────────────

    suspend fun generateAiReply(businessId: String, req: AiReplyRequest): ApiResponse<AiReplyResponse> {
        val detail = transaction {
            getConversationDetail(businessId, req.conversationId)
        } ?: return ApiResponse(false, message = "Conversation not found")

        val channel = transaction {
            val convRow = SocialConversationsTable.select { SocialConversationsTable.id eq req.conversationId }.firstOrNull()
            convRow?.let { SocialChannelsTable.select { SocialChannelsTable.id eq it[SocialConversationsTable.channelId] }.firstOrNull() }
        }

        val products = productService.getAll(businessId, null, false)
        val productCatalog = products.take(30).joinToString("\n") {
            "- ${it.name} (KES ${it.sellingPrice}, stock: ${it.currentStock})"
        }

        val persona = req.customInstruction
            ?: channel?.get(SocialChannelsTable.aiPersonaPrompt)?.takeIf { it.isNotBlank() }
            ?: "You are a helpful sales agent for a Kenyan business. Be friendly, use Swahili/English mix (Sheng optional). Keep replies short (under 100 words). Always try to help customers place orders."

        val conversationHistory = detail.messages.takeLast(10).joinToString("\n") {
            "${if (it.direction == "INBOUND") "Customer" else "Agent"}: ${it.content}"
        }

        val prompt = """
$persona

PRODUCT CATALOG:
$productCatalog

RECENT CONVERSATION:
$conversationHistory

Based on the conversation, generate a helpful reply. Also analyse the customer's intent.
Respond ONLY with a JSON object in this exact format (no markdown, no extra text):
{
  "reply": "your message here",
  "intent": "INQUIRY|ORDER|COMPLAINT|GREETING|PAYMENT_FOLLOWUP|OTHER",
  "detectedProducts": [{"productName": "...", "confidence": 0.9, "quantity": 1}],
  "shouldCreateOrder": false,
  "paymentPromptSuggested": false,
  "confidence": 0.9
}
""".trimIndent()

        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", System.getenv("ANTHROPIC_API_KEY") ?: "")
                header("anthropic-version", "2023-06-01")
                setBody(Json.encodeToString(buildJsonObject {
                    put("model", "claude-sonnet-4-20250514")
                    put("max_tokens", 500)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    }
                }))
            }
            val raw  = response.bodyAsText()
            val json = Json.parseToJsonElement(raw).jsonObject
            val text = json["content"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "{}"
            val parsed = Json.parseToJsonElement(text.trim()).jsonObject

            val detectedProducts = parsed["detectedProducts"]?.jsonArray?.mapNotNull { item ->
                val obj = item.jsonObject
                ProductMention(
                    productId   = null,
                    productName = obj["productName"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    confidence  = obj["confidence"]?.jsonPrimitive?.double ?: 0.5,
                    quantity    = obj["quantity"]?.jsonPrimitive?.intOrNull
                )
            } ?: emptyList()

            ApiResponse(true, data = AiReplyResponse(
                suggestedReply          = parsed["reply"]?.jsonPrimitive?.content ?: "",
                detectedIntent          = parsed["intent"]?.jsonPrimitive?.content ?: "OTHER",
                detectedProducts        = detectedProducts,
                shouldCreateOrder       = parsed["shouldCreateOrder"]?.jsonPrimitive?.boolean ?: false,
                paymentPromptSuggested  = parsed["paymentPromptSuggested"]?.jsonPrimitive?.boolean ?: false,
                confidence              = parsed["confidence"]?.jsonPrimitive?.double ?: 0.8
            ))
        } catch (e: Exception) {
            ApiResponse(false, message = "AI reply error: ${e.message}")
        }
    }

    // ── Send Payment Prompt ───────────────────────────────────────────────────

    suspend fun sendPaymentPrompt(businessId: String, req: SendPaymentPromptRequest): ApiResponse<PaymentPromptResponse> {
        val conv = transaction {
            SocialConversationsTable.select {
                (SocialConversationsTable.id eq req.conversationId) and
                (SocialConversationsTable.businessId eq businessId)
            }.firstOrNull()
        } ?: return ApiResponse(false, message = "Conversation not found")

        val channel = transaction {
            SocialChannelsTable.select { SocialChannelsTable.id eq conv[SocialConversationsTable.channelId] }.firstOrNull()
        } ?: return ApiResponse(false, message = "Channel not found")

        val customerName  = conv[SocialConversationsTable.customerName]
        val customerPhone = conv[SocialConversationsTable.customerPhone]

        // Build payment message (Mpesa STK or manual paybill)
        val mpesaPaybill = channel[SocialChannelsTable.phoneNumber]?.let {
            "💳 *Lipa kwa Mpesa:*\nPaybill: 174379\nAccount: ${req.orderId ?: "ORDER"}\nKiasi: KES ${"%,.0f".format(req.amount)}"
        }

        val paymentMsg = buildString {
            append("Hujambo $customerName! 🛍️\n\n")
            append("*${req.description}*\n")
            append("💰 Jumla: *KES ${"%,.0f".format(req.amount)}*\n\n")
            if (mpesaPaybill != null) {
                append("$mpesaPaybill\n\n")
            }
            append("Au tuma pesa kwa:\n📱 M-Pesa: ${channel[SocialChannelsTable.phoneNumber] ?: "0700000000"}\n\n")
            append("Asante kwa kununua! 🙏")
        }

        // Send the payment message
        val sendReq = SendMessageRequest(req.conversationId, paymentMsg, "PAYMENT_REQUEST")
        val sendResult = sendMessage(businessId, sendReq)

        // Trigger STK push if we have the customer's phone
        var checkoutRequestId: String? = null
        var stkPushed = false
        if (customerPhone != null && req.paymentMethod != "CARD") {
            try {
                val stkResult = mpesaService.initiateSTKPush(
                    phoneNumber      = customerPhone,
                    amount           = req.amount,
                    accountReference = req.orderId ?: "SOCIAL_ORDER",
                    transactionDesc  = req.description.take(30)
                )
                if (stkResult is StkPushResult.Success) {
                    checkoutRequestId = stkResult.checkoutRequestId
                    stkPushed = true
                }
            } catch (_: Exception) {}
        }

        // Update conversation status
        transaction {
            SocialConversationsTable.update({ SocialConversationsTable.id eq req.conversationId }) {
                it[status] = "PENDING_PAYMENT"
            }
        }

        return ApiResponse(true, data = PaymentPromptResponse(
            messageId              = sendResult.data?.id ?: "",
            paymentMessage         = paymentMsg,
            mpesaPaybillMessage    = mpesaPaybill,
            stkPushInitiated       = stkPushed,
            checkoutRequestId      = checkoutRequestId
        ))
    }

    // ── Create Order from Social ──────────────────────────────────────────────

    suspend fun createSocialOrder(businessId: String, req: CreateSocialOrderRequest): ApiResponse<OrderResponse> {
        val orderReq = CreateOrderRequest(
            customerName     = req.customerName,
            customerPhone    = req.customerPhone,
            deliveryLocation = req.deliveryLocation,
            items            = req.items,
            paymentMethod    = req.paymentMethod,
            notes            = "Order from ${transaction { SocialConversationsTable.select { SocialConversationsTable.id eq req.conversationId }.firstOrNull()?.get(SocialConversationsTable.platform) } ?: "Social"}"
        )
        val result = orderService.create(businessId, orderReq)
        if (result.success && result.data != null) {
            val orderId = result.data.id
            transaction {
                SocialOrdersTable.insert {
                    it[id]             = generateId()
                    it[SocialOrdersTable.businessId]     = businessId
                    it[conversationId] = req.conversationId
                    it[SocialOrdersTable.orderId]        = orderId
                    it[platform]       = SocialConversationsTable.select { SocialConversationsTable.id eq req.conversationId }
                        .firstOrNull()?.get(SocialConversationsTable.platform) ?: "UNKNOWN"
                    it[createdAt]      = Clock.System.now()
                }
                SocialConversationsTable.update({ SocialConversationsTable.id eq req.conversationId }) {
                    it[assignedOrderId] = orderId
                    it[status]          = "PENDING_PAYMENT"
                }
            }

            if (req.sendPaymentPromptImmediately) {
                sendPaymentPrompt(businessId, SendPaymentPromptRequest(
                    conversationId = req.conversationId,
                    orderId        = orderId,
                    amount         = result.data.subtotal,
                    description    = "Order ${result.data.orderNumber}"
                ))
            }
        }
        return result
    }

    // ── Inbox Stats ───────────────────────────────────────────────────────────

    fun getInboxStats(businessId: String): SocialInboxStats = transaction {
        val platforms = listOf("WHATSAPP", "INSTAGRAM", "FACEBOOK", "TIKTOK")
        val channels  = SocialChannelsTable.select { SocialChannelsTable.businessId eq businessId }
            .map { it[SocialChannelsTable.platform] to it[SocialChannelsTable.isActive] }.toMap()

        val platformStats = platforms.map { p ->
            val convs  = SocialConversationsTable.select { (SocialConversationsTable.businessId eq businessId) and (SocialConversationsTable.platform eq p) }.count().toInt()
            val orders = SocialOrdersTable.select { (SocialOrdersTable.businessId eq businessId) and (SocialOrdersTable.platform eq p) }.count().toInt()
            PlatformStat(p, convs, orders, 0.0, channels[p] == true)
        }

        SocialInboxStats(
            totalConversations = SocialConversationsTable.select { SocialConversationsTable.businessId eq businessId }.count().toInt(),
            openConversations  = SocialConversationsTable.select { (SocialConversationsTable.businessId eq businessId) and (SocialConversationsTable.status eq "OPEN") }.count().toInt(),
            pendingPayment     = SocialConversationsTable.select { (SocialConversationsTable.businessId eq businessId) and (SocialConversationsTable.status eq "PENDING_PAYMENT") }.count().toInt(),
            aiHandled          = SocialConversationsTable.select { (SocialConversationsTable.businessId eq businessId) and (SocialConversationsTable.isAiHandled eq true) }.count().toInt(),
            ordersFromSocial   = SocialOrdersTable.select { SocialOrdersTable.businessId eq businessId }.count().toInt(),
            revenueFromSocial  = 0.0,
            byPlatform         = platformStats
        )
    }

    // ── Inbound Webhook Processing ────────────────────────────────────────────

    suspend fun processMetaWebhook(payload: MetaWebhookPayload) {
        payload.entry.forEach { entry ->
            entry.changes?.forEach { change ->
                when {
                    change.field == "messages" -> processWhatsAppMessage(entry.id, change.value)
                    change.field == "instagram_messages" -> processInstagramMessage(entry.id, change.value)
                    change.field == "feed" -> processFacebookComment(entry.id, change.value)
                }
            }
            entry.messaging?.forEach { msg ->
                processFacebookMessengerMessage(entry.id, msg)
            }
        }
    }

    private suspend fun processWhatsAppMessage(wabaId: String, value: MetaChangeValue) {
        val messages = value.messages ?: return
        val contacts = value.contacts ?: emptyList()

        messages.forEach { msg ->
            if (msg.type != "text") return@forEach
            val senderName = contacts.find { it.wa_id == msg.from }?.profile?.name ?: "WhatsApp User"
            val content    = msg.text?.body ?: return@forEach
            val channel    = findChannelByExternalId("WHATSAPP", wabaId) ?: return@forEach

            val convId = upsertConversation(channel, "WHATSAPP", msg.from, msg.id, senderName, null)
            saveInboundMessage(convId, channel[SocialChannelsTable.businessId], msg.id, content, "TEXT")

            if (channel[SocialChannelsTable.autoReplyEnabled]) {
                autoReplyIfNeeded(channel, convId, content)
            }
        }
    }

    private suspend fun processInstagramMessage(pageId: String, value: MetaChangeValue) {
        val senderId = value.from ?: return
        val content  = value.text ?: return
        val msgId    = value.id ?: generateId()
        val channel  = findChannelByExternalId("INSTAGRAM", pageId) ?: return
        val convId   = upsertConversation(channel, "INSTAGRAM", senderId, msgId, "Instagram User", null)
        saveInboundMessage(convId, channel[SocialChannelsTable.businessId], msgId, content, "TEXT")
        if (channel[SocialChannelsTable.autoReplyEnabled]) autoReplyIfNeeded(channel, convId, content)
    }

    private suspend fun processFacebookComment(pageId: String, value: MetaChangeValue) {
        // Facebook post comments — reply to comment thread
        val senderId = value.from ?: return
        val content  = value.text ?: return
        val msgId    = value.id ?: generateId()
        val channel  = findChannelByExternalId("FACEBOOK", pageId) ?: return
        val convId   = upsertConversation(channel, "FACEBOOK", senderId, msgId, "Facebook User", null)
        saveInboundMessage(convId, channel[SocialChannelsTable.businessId], msgId, "Comment: $content", "TEXT")
        if (channel[SocialChannelsTable.autoReplyEnabled]) autoReplyIfNeeded(channel, convId, content)
    }

    private suspend fun processFacebookMessengerMessage(pageId: String, msg: MetaMessaging) {
        val senderId = msg.sender.id
        val content  = msg.message?.text ?: return
        val msgId    = msg.message.mid
        val channel  = findChannelByExternalId("FACEBOOK", pageId) ?: return
        val convId   = upsertConversation(channel, "FACEBOOK", senderId, msgId, "Messenger User", null)
        saveInboundMessage(convId, channel[SocialChannelsTable.businessId], msgId, content, "TEXT")
        if (channel[SocialChannelsTable.autoReplyEnabled]) autoReplyIfNeeded(channel, convId, content)
    }

    suspend fun processTikTokWebhook(channelId: String, payload: TikTokWebhookPayload) {
        val channel  = transaction { SocialChannelsTable.select { SocialChannelsTable.id eq channelId }.firstOrNull() } ?: return
        val content  = payload.data.content
        val senderId = payload.data.from_user_id
        val msgId    = payload.data.message_id ?: generateId()
        val prefix   = if (payload.event == "comment") "Comment: " else ""
        val convId   = upsertConversation(channel, "TIKTOK", senderId, msgId, "TikTok User", null)
        saveInboundMessage(convId, channel[SocialChannelsTable.businessId], msgId, "$prefix$content", "TEXT")
        if (channel[SocialChannelsTable.autoReplyEnabled]) autoReplyIfNeeded(channel, convId, content)
    }

    // ── Platform API Senders ──────────────────────────────────────────────────

    private suspend fun sendWhatsAppMessage(token: String, wabaPhoneId: String, to: String, text: String): Boolean {
        return try {
            val r = httpClient.post("https://graph.facebook.com/v20.0/$wabaPhoneId/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""{"messaging_product":"whatsapp","to":"$to","type":"text","text":{"body":${Json.encodeToString(text)}}}""")
            }
            r.status.isSuccess()
        } catch (_: Exception) { false }
    }

    private suspend fun sendInstagramDm(token: String, igAccountId: String, recipientId: String, text: String): Boolean {
        return try {
            val r = httpClient.post("https://graph.facebook.com/v20.0/$igAccountId/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""{"recipient":{"id":"$recipientId"},"message":{"text":${Json.encodeToString(text)}}}""")
            }
            r.status.isSuccess()
        } catch (_: Exception) { false }
    }

    private suspend fun sendFacebookMessage(token: String, pageId: String, recipientId: String, text: String): Boolean {
        return try {
            val r = httpClient.post("https://graph.facebook.com/v20.0/$pageId/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""{"recipient":{"id":"$recipientId"},"message":{"text":${Json.encodeToString(text)}}}""")
            }
            r.status.isSuccess()
        } catch (_: Exception) { false }
    }

    private suspend fun sendTikTokDm(token: String, conversationId: String, text: String): Boolean {
        return try {
            val r = httpClient.post("https://open.tiktokapis.com/v2/business/dm/conversation/message/send/") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""{"conversation_id":"$conversationId","message_type":"TEXT","content":{"text":${Json.encodeToString(text)}}}""")
            }
            r.status.isSuccess()
        } catch (_: Exception) { false }
    }

    // ── Channel Settings Update ───────────────────────────────────────────────

    fun updateChannelSettings(businessId: String, channelId: String, req: SocialChannelRequest): ApiResponse<SocialChannelResponse> = transaction {
        val updated = SocialChannelsTable.update({
            (SocialChannelsTable.id eq channelId) and (SocialChannelsTable.businessId eq businessId)
        }) {
            it[autoReplyEnabled] = req.autoReplyEnabled
            it[aiPersonaPrompt]  = req.aiPersonaPrompt
            it[channelName]      = req.channelName
            it[updatedAt]        = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Channel not found")
        val row = SocialChannelsTable.select { SocialChannelsTable.id eq channelId }.first()
        ApiResponse(true, data = row.toChannelResponse(businessId))
    }

    // ── Conversation Status Update ─────────────────────────────────────────────

    fun updateConversationStatus(businessId: String, conversationId: String, newStatus: String): ApiResponse<Unit> = transaction {
        val valid = listOf("OPEN","PENDING_PAYMENT","COMPLETED","CLOSED")
        if (newStatus.uppercase() !in valid)
            return@transaction ApiResponse(false, message = "Status must be one of: ${valid.joinToString()}")
        val updated = SocialConversationsTable.update({
            (SocialConversationsTable.id eq conversationId) and (SocialConversationsTable.businessId eq businessId)
        }) { it[status] = newStatus.uppercase() }
        if (updated == 0) ApiResponse(false, message = "Conversation not found")
        else ApiResponse(true, message = "Status updated")
    }

    // ── Webhook Token Verification ─────────────────────────────────────────────

    fun verifyWebhookToken(platform: String, token: String): Boolean = transaction {
        SocialChannelsTable.select {
            (SocialChannelsTable.platform eq platform.uppercase()) and
            (SocialChannelsTable.webhookVerifyToken eq token) and
            (SocialChannelsTable.isActive eq true)
        }.count() > 0L
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun autoReplyIfNeeded(channel: ResultRow, convId: String, inboundText: String) {
        val businessId = channel[SocialChannelsTable.businessId]
        val aiResult   = generateAiReply(businessId, AiReplyRequest(convId))
        if (aiResult.success && aiResult.data != null) {
            val reply = aiResult.data.suggestedReply
            if (reply.isNotBlank()) {
                val now = Clock.System.now()
                transaction {
                    SocialMessagesTable.insert {
                        it[id]             = generateId()
                        it[conversationId] = convId
                        it[SocialMessagesTable.businessId] = businessId
                        it[direction]      = "OUTBOUND"
                        it[senderType]     = "AI"
                        it[content]        = reply
                        it[messageType]    = "TEXT"
                        it[status]         = "SENT"
                        it[isAiGenerated]  = true
                        it[createdAt]      = now
                    }
                    SocialConversationsTable.update({ SocialConversationsTable.id eq convId }) {
                        it[isAiHandled]   = true
                        it[lastMessageAt] = now
                    }
                }
                // Actually send the reply via platform
                val conv = transaction { SocialConversationsTable.select { SocialConversationsTable.id eq convId }.first() }
                when (channel[SocialChannelsTable.platform]) {
                    "WHATSAPP"  -> sendWhatsAppMessage(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], conv[SocialConversationsTable.customerExternalId], reply)
                    "INSTAGRAM" -> sendInstagramDm(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], conv[SocialConversationsTable.customerExternalId], reply)
                    "FACEBOOK"  -> sendFacebookMessage(channel[SocialChannelsTable.accessToken], channel[SocialChannelsTable.externalId], conv[SocialConversationsTable.customerExternalId], reply)
                    "TIKTOK"    -> sendTikTokDm(channel[SocialChannelsTable.accessToken], conv[SocialConversationsTable.externalConvId], reply)
                }
            }
        }
    }

    private fun findChannelByExternalId(platform: String, externalId: String): ResultRow? = transaction {
        SocialChannelsTable.select {
            (SocialChannelsTable.platform eq platform) and
            (SocialChannelsTable.externalId eq externalId) and
            (SocialChannelsTable.isActive eq true)
        }.firstOrNull()
    }

    private fun upsertConversation(channel: ResultRow, platform: String, senderId: String, externalMsgId: String, senderName: String, phone: String?): String = transaction {
        val channelId  = channel[SocialChannelsTable.id]
        val businessId = channel[SocialChannelsTable.businessId]
        val convKey    = "$channelId:$senderId"
        val existing   = SocialConversationsTable.select {
            (SocialConversationsTable.channelId eq channelId) and
            (SocialConversationsTable.customerExternalId eq senderId)
        }.firstOrNull()

        val now = Clock.System.now()
        if (existing != null) {
            SocialConversationsTable.update({ SocialConversationsTable.id eq existing[SocialConversationsTable.id] }) {
                it[lastMessageAt] = now
                it[unreadCount] = existing[SocialConversationsTable.unreadCount] + 1
            }
            existing[SocialConversationsTable.id]
        } else {
            val id = generateId()
            SocialConversationsTable.insert {
                it[SocialConversationsTable.id]            = id
                it[SocialConversationsTable.businessId]    = businessId
                it[SocialConversationsTable.channelId]     = channelId
                it[SocialConversationsTable.platform]      = platform
                it[externalConvId]                         = externalMsgId
                it[customerExternalId]                     = senderId
                it[customerName]                           = senderName
                it[customerPhone]                          = phone
                it[lastMessageAt]                          = now
                it[unreadCount]                            = 1
                it[createdAt]                              = now
            }
            id
        }
    }

    private fun saveInboundMessage(convId: String, businessId: String, externalMsgId: String, content: String, type: String) = transaction {
        SocialMessagesTable.insert {
            it[id]             = generateId()
            it[conversationId] = convId
            it[SocialMessagesTable.businessId] = businessId
            it[SocialMessagesTable.externalMsgId] = externalMsgId
            it[direction]      = "INBOUND"
            it[senderType]     = "CUSTOMER"
            it[SocialMessagesTable.content]    = content
            it[messageType]    = type
            it[status]         = "DELIVERED"
            it[createdAt]      = Clock.System.now()
        }
    }

    private fun ResultRow.toChannelResponse(businessId: String): SocialChannelResponse {
        val unread = transaction {
            SocialConversationsTable.select {
                (SocialConversationsTable.businessId eq businessId) and
                (SocialConversationsTable.channelId eq this@toChannelResponse[SocialChannelsTable.id])
            }.sumOf { it[SocialConversationsTable.unreadCount] }
        }
        val token = this[SocialChannelsTable.webhookVerifyToken]
        return SocialChannelResponse(
            id                 = this[SocialChannelsTable.id],
            platform           = this[SocialChannelsTable.platform],
            channelName        = this[SocialChannelsTable.channelName],
            externalId         = this[SocialChannelsTable.externalId],
            phoneNumber        = this[SocialChannelsTable.phoneNumber],
            isActive           = this[SocialChannelsTable.isActive],
            autoReplyEnabled   = this[SocialChannelsTable.autoReplyEnabled],
            aiPersonaPrompt    = this[SocialChannelsTable.aiPersonaPrompt],
            webhookVerifyToken = token,
            webhookUrl         = "https://api.biashara360.co.ke/v1/social/webhook/${this[SocialChannelsTable.platform].lowercase()}",
            unreadCount        = unread,
            createdAt          = this[SocialChannelsTable.createdAt].toString()
        )
    }
}
