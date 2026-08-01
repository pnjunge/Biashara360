package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.security.TokenCipher
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
    private val productService: ProductService,
    config: ApplicationConfig
) {
    private val metaSystemUserToken =
        config.propertyOrNull("social.metaSystemUserToken")?.getString()?.trim().orEmpty()
    private val metaAppId =
        config.propertyOrNull("facebook.appId")?.getString()?.trim().orEmpty()
    private val metaAppSecret =
        config.propertyOrNull("facebook.appSecret")?.getString()?.trim().orEmpty()
    private val metaEmbeddedSignupConfigurationId =
        config.propertyOrNull("facebook.embeddedSignupConfigurationId")?.getString()?.trim().orEmpty()
    private val metaWebhookVerifyToken =
        config.propertyOrNull("facebook.webhookVerifyToken")?.getString()?.trim().orEmpty()
    private val tokenCipher = config.propertyOrNull("social.tokenEncryptionKey")
        ?.getString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::TokenCipher)
    private val graphApiVersion = config.propertyOrNull("facebook.graphApiVersion")
        ?.getString()
        ?.trim()
        ?.takeIf { it.matches(Regex("""v\d+\.\d+""")) }
        ?: "v25.0"

    fun isMetaSystemUserConfigured(): Boolean = metaSystemUserToken.isNotBlank()

    fun getMetaOnboardingConfiguration(): MetaOnboardingConfigurationResponse {
        val missing = buildList {
            if (metaAppId.isBlank()) add("META_APP_ID")
            if (metaAppSecret.isBlank()) add("META_APP_SECRET")
            if (metaEmbeddedSignupConfigurationId.isBlank()) add("META_EMBEDDED_SIGNUP_CONFIG_ID")
            if (metaWebhookVerifyToken.isBlank()) add("META_WEBHOOK_VERIFY_TOKEN")
            if (tokenCipher == null) add("SOCIAL_TOKEN_ENCRYPTION_KEY")
        }
        return MetaOnboardingConfigurationResponse(
            configured = missing.isEmpty(),
            appId = metaAppId.takeIf { it.isNotBlank() },
            configurationId = metaEmbeddedSignupConfigurationId.takeIf { it.isNotBlank() },
            graphApiVersion = graphApiVersion,
            missing = missing
        )
    }

    // ── Channel Management ────────────────────────────────────────────────────

    fun getChannels(businessId: String): List<SocialChannelResponse> = transaction {
        SocialChannelsTable.select { SocialChannelsTable.businessId eq businessId }
            .map { it.toChannelResponse(businessId) }
    }

    fun connectChannel(businessId: String, req: SocialChannelRequest): ApiResponse<SocialChannelResponse> = transaction {
        val platformName = req.platform.uppercase()
        if (platformName == "TIKTOK") {
            return@transaction ApiResponse(
                false,
                message = "TikTok messaging onboarding is not available yet"
            )
        }
        if (platformName !in setOf("WHATSAPP", "INSTAGRAM", "FACEBOOK")) {
            return@transaction ApiResponse(false, message = "Unsupported social platform")
        }
        if (platformName == "WHATSAPP") {
            if (!isMetaSystemUserConfigured()) {
                return@transaction ApiResponse(false, message = "Platform WhatsApp credentials are not configured")
            }
            if (req.wabaId.isNullOrBlank() || req.phoneNumberId.isNullOrBlank() || req.metaBusinessId.isNullOrBlank()) {
                return@transaction ApiResponse(false, message = "wabaId, phoneNumberId, and metaBusinessId are required")
            }
            val duplicate = SocialChannelsTable.select {
                (SocialChannelsTable.platform eq "WHATSAPP") and
                    ((SocialChannelsTable.wabaId eq req.wabaId) or
                        (SocialChannelsTable.phoneNumberId eq req.phoneNumberId))
            }.count() > 0
            if (duplicate) {
                return@transaction ApiResponse(false, message = "This WhatsApp Business Account or phone number is already connected")
            }
        } else {
            if (req.externalId.isBlank()) {
                return@transaction ApiResponse(false, message = "Meta account ID is required")
            }
            if (req.accessToken.isBlank()) {
                return@transaction ApiResponse(false, message = "Meta access token is required")
            }
            if (tokenCipher == null) {
                return@transaction ApiResponse(
                    false,
                    message = "Secure social credential storage is not configured"
                )
            }
            val duplicate = SocialChannelsTable.select {
                (SocialChannelsTable.platform eq platformName) and
                    (SocialChannelsTable.externalId eq req.externalId) and
                    (SocialChannelsTable.businessId eq businessId) and
                    (SocialChannelsTable.isActive eq true)
            }.count() > 0
            if (duplicate) {
                return@transaction ApiResponse(false, message = "This account is already connected")
            }
        }
        val id           = generateId()
        val verifyToken  = UUID.randomUUID().toString().replace("-", "")
        val now          = Clock.System.now()
        SocialChannelsTable.insert {
            it[SocialChannelsTable.id]           = id
            it[SocialChannelsTable.businessId]   = businessId
            it[platform]                         = platformName
            it[channelName]                      = req.channelName
            it[externalId]                       = if (platformName == "WHATSAPP") req.phoneNumberId!! else req.externalId
            it[phoneNumber]                      = req.phoneNumber
            it[tenantId]                         = if (platformName == "WHATSAPP") businessId else null
            it[wabaId]                           = req.wabaId
            it[phoneNumberId]                    = req.phoneNumberId
            it[metaBusinessId]                   = req.metaBusinessId
            it[accessToken]                      = if (platformName == "WHATSAPP") "" else tokenCipher!!.encrypt(req.accessToken)
            it[refreshToken]                     = if (platformName == "WHATSAPP") {
                null
            } else {
                req.refreshToken?.takeIf(String::isNotBlank)?.let(tokenCipher!!::encrypt)
            }
            it[isActive]                        = platformName == "WHATSAPP"
            it[connectionStatus]                = if (platformName == "WHATSAPP") "CONNECTED" else "ACTION_REQUIRED"
            it[tokenEncryptionVersion]          = if (platformName == "WHATSAPP") 0 else 1
            it[autoReplyEnabled]                 = req.autoReplyEnabled
            it[aiPersonaPrompt]                  = req.aiPersonaPrompt
            it[webhookVerifyToken]               = verifyToken
            it[createdAt]                        = now
            it[updatedAt]                        = now
        }
        val channel = SocialChannelsTable.select { SocialChannelsTable.id eq id }.first()
        ApiResponse(
            true,
            data = channel.toChannelResponse(businessId),
            message = if (platformName == "WHATSAPP") {
                "WhatsApp channel connected"
            } else {
                "$platformName credentials saved; verification required"
            }
        )
    }

    suspend fun completeMetaEmbeddedSignup(
        businessId: String,
        req: MetaEmbeddedSignupRequest
    ): ApiResponse<SocialChannelResponse> {
        val config = getMetaOnboardingConfiguration()
        if (!config.configured) {
            return ApiResponse(false, message = "Meta onboarding is not configured")
        }
        if (!req.wabaId.isMetaId() || !req.phoneNumberId.isMetaId() || !req.metaBusinessId.isMetaId()) {
            return ApiResponse(false, message = "Meta returned invalid business asset identifiers")
        }
        if (req.code.isBlank() || req.code.length > 4096) {
            return ApiResponse(false, message = "Meta authorization code is missing or invalid")
        }
        val alreadyAssigned = transaction {
            SocialChannelsTable.select {
                (SocialChannelsTable.platform eq "WHATSAPP") and
                    (SocialChannelsTable.phoneNumberId eq req.phoneNumberId) and
                    (SocialChannelsTable.businessId neq businessId) and
                    (SocialChannelsTable.isActive eq true)
            }.count() > 0
        }
        if (alreadyAssigned) {
            return ApiResponse(false, message = "This WhatsApp number is already connected to another Biashara360 business")
        }

        return try {
            val tokenResponse = httpClient.get("https://graph.facebook.com/$graphApiVersion/oauth/access_token") {
                parameter("client_id", metaAppId)
                parameter("client_secret", metaAppSecret)
                parameter("code", req.code)
            }
            val tokenJson = tokenResponse.safeGraphJson()
            if (!tokenResponse.status.isSuccess()) {
                return ApiResponse(false, message = tokenJson.graphErrorMessage("Meta authorization failed"))
            }
            val accessToken = tokenJson.string("access_token")
                ?: return ApiResponse(false, message = "Meta did not return an access token")

            val wabaResponse = graphGet(req.wabaId, accessToken, "id,name,owner_business_info")
            if (!wabaResponse.first) {
                return ApiResponse(false, message = wabaResponse.second.graphErrorMessage("Unable to access the selected WhatsApp account"))
            }
            val ownerBusinessId = wabaResponse.second["owner_business_info"]
                ?.jsonObject
                ?.string("id")
            if (ownerBusinessId != null && ownerBusinessId != req.metaBusinessId) {
                return ApiResponse(false, message = "The selected WhatsApp account does not belong to the authorized Meta business")
            }

            val phonesResponse = graphGet("${req.wabaId}/phone_numbers", accessToken,
                "id,display_phone_number,verified_name,code_verification_status,platform_type")
            if (!phonesResponse.first) {
                return ApiResponse(false, message = phonesResponse.second.graphErrorMessage("Unable to access WhatsApp phone numbers"))
            }
            val phoneJson = phonesResponse.second["data"]?.jsonArray
                ?.firstOrNull { it.jsonObject.string("id") == req.phoneNumberId }
                ?.jsonObject
                ?: return ApiResponse(false, message = "The selected phone number does not belong to the authorized WhatsApp account")

            val registrationPin = "%06d".format(SecureRandom().nextInt(1_000_000))
            val registerResponse = httpClient.post(
                "https://graph.facebook.com/$graphApiVersion/${req.phoneNumberId}/register"
            ) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("messaging_product", "whatsapp")
                    put("pin", registrationPin)
                }.toString())
            }
            val registerJson = registerResponse.safeGraphJson()
            if (!registerResponse.status.isSuccess() ||
                registerJson["success"]?.jsonPrimitive?.booleanOrNull != true
            ) {
                return ApiResponse(false, message = registerJson.graphErrorMessage("Could not register the WhatsApp phone number"))
            }

            val subscribeResponse = httpClient.post("https://graph.facebook.com/$graphApiVersion/${req.wabaId}/subscribed_apps") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            val subscribeJson = subscribeResponse.safeGraphJson()
            if (!subscribeResponse.status.isSuccess() || subscribeJson["success"]?.jsonPrimitive?.booleanOrNull != true) {
                return ApiResponse(false, message = subscribeJson.graphErrorMessage("Could not subscribe WhatsApp webhooks"))
            }

            val encryptedToken = tokenCipher!!.encrypt(accessToken)
            val encryptedPin = tokenCipher.encrypt(registrationPin)
            val now = Clock.System.now()
            val displayPhone = phoneJson.string("display_phone_number")
            val verifiedName = phoneJson.string("verified_name")
            val name = req.channelName?.trim()?.takeIf { it.isNotEmpty() }
                ?: verifiedName?.takeIf { it.isNotBlank() }
                ?: "WhatsApp Business"

            val row = transaction {
                val owned = SocialChannelsTable.select {
                    (SocialChannelsTable.platform eq "WHATSAPP") and
                        (SocialChannelsTable.phoneNumberId eq req.phoneNumberId) and
                        (SocialChannelsTable.businessId eq businessId)
                }.firstOrNull()
                val channelId = owned?.get(SocialChannelsTable.id) ?: generateId()
                if (owned == null) {
                    SocialChannelsTable.insert {
                        it[id] = channelId
                        it[SocialChannelsTable.businessId] = businessId
                        it[platform] = "WHATSAPP"
                        it[channelName] = name
                        it[externalId] = req.phoneNumberId
                        it[phoneNumber] = displayPhone
                        it[tenantId] = businessId
                        it[wabaId] = req.wabaId
                        it[phoneNumberId] = req.phoneNumberId
                        it[metaBusinessId] = req.metaBusinessId
                        it[SocialChannelsTable.accessToken] = encryptedToken
                        it[refreshToken] = null
                        it[webhookVerifyToken] = metaWebhookVerifyToken
                        it[isActive] = true
                        it[connectionStatus] = "CONNECTED"
                        it[onboardingMethod] = "META_EMBEDDED_SIGNUP"
                        it[tokenEncryptionVersion] = 1
                        it[registrationPinEncrypted] = encryptedPin
                        it[lastVerifiedAt] = now
                        it[disconnectedAt] = null
                        it[autoReplyEnabled] = false
                        it[aiPersonaPrompt] = ""
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                } else {
                    SocialChannelsTable.update({ SocialChannelsTable.id eq channelId }) {
                        it[channelName] = name
                        it[externalId] = req.phoneNumberId
                        it[phoneNumber] = displayPhone
                        it[tenantId] = businessId
                        it[wabaId] = req.wabaId
                        it[phoneNumberId] = req.phoneNumberId
                        it[metaBusinessId] = req.metaBusinessId
                        it[SocialChannelsTable.accessToken] = encryptedToken
                        it[refreshToken] = null
                        it[isActive] = true
                        it[connectionStatus] = "CONNECTED"
                        it[onboardingMethod] = "META_EMBEDDED_SIGNUP"
                        it[tokenEncryptionVersion] = 1
                        it[registrationPinEncrypted] = encryptedPin
                        it[lastVerifiedAt] = now
                        it[disconnectedAt] = null
                        it[updatedAt] = now
                    }
                }
                SocialChannelsTable.select { SocialChannelsTable.id eq channelId }.first()
            }
            ApiResponse(true, data = row.toChannelResponse(businessId), message = "WhatsApp connected")
        } catch (exception: Exception) {
            val safeMessage = exception.message
                ?.takeIf { it.startsWith("This WhatsApp number") }
                ?: "Unable to complete WhatsApp onboarding"
            ApiResponse(false, message = safeMessage)
        }
    }

    suspend fun verifyChannelConnection(
        businessId: String,
        channelId: String
    ): ApiResponse<SocialConnectionVerificationResponse> {
        val channel = transaction {
            SocialChannelsTable.select {
                (SocialChannelsTable.id eq channelId) and
                    (SocialChannelsTable.businessId eq businessId)
            }.firstOrNull()
        } ?: return ApiResponse(false, message = "Channel not found")
        val platform = channel[SocialChannelsTable.platform]
        if (platform == "TIKTOK") {
            return ApiResponse(false, message = "TikTok messaging onboarding is not available yet")
        }
        return try {
            val token = channelToken(channel)
            val accountId = if (platform == "WHATSAPP") {
                channel[SocialChannelsTable.phoneNumberId] ?: channel[SocialChannelsTable.externalId]
            } else {
                channel[SocialChannelsTable.externalId]
            }
            val fields = when (platform) {
                "WHATSAPP" -> "id,display_phone_number,verified_name,quality_rating"
                "INSTAGRAM" -> "id,username"
                "FACEBOOK" -> "id,name"
                else -> return ApiResponse(false, message = "Unsupported social platform")
            }
            val response = graphGet(accountId, token, fields)
            val connected = response.first && response.second.string("id") == accountId
            val now = Clock.System.now()
            transaction {
                SocialChannelsTable.update({ SocialChannelsTable.id eq channelId }) {
                    it[connectionStatus] = if (connected) "CONNECTED" else "ACTION_REQUIRED"
                    it[isActive] = connected
                    if (connected) it[lastVerifiedAt] = now
                    it[updatedAt] = now
                }
            }
            ApiResponse(
                success = connected,
                data = SocialConnectionVerificationResponse(
                    connected = connected,
                    connectionStatus = if (connected) "CONNECTED" else "ACTION_REQUIRED",
                    phoneNumber = response.second.string("display_phone_number"),
                    displayName = response.second.string("verified_name")
                        ?: response.second.string("username")
                        ?: response.second.string("name")
                ),
                message = if (connected) "$platform connection verified" else "$platform authorization needs attention"
            )
        } catch (_: Exception) {
            transaction {
                SocialChannelsTable.update({ SocialChannelsTable.id eq channelId }) {
                    it[connectionStatus] = "ACTION_REQUIRED"
                    it[isActive] = false
                    it[updatedAt] = Clock.System.now()
                }
            }
            ApiResponse(
                false,
                data = SocialConnectionVerificationResponse(false, "ACTION_REQUIRED"),
                message = "$platform authorization needs attention"
            )
        }
    }

    suspend fun disconnectChannel(businessId: String, channelId: String): ApiResponse<Unit> {
        val channel = transaction {
            SocialChannelsTable.select {
                (SocialChannelsTable.id eq channelId) and
                    (SocialChannelsTable.businessId eq businessId)
            }.firstOrNull()
        } ?: return ApiResponse(false, message = "Channel not found")

        if (channel[SocialChannelsTable.platform] == "WHATSAPP" &&
            channel[SocialChannelsTable.onboardingMethod] == "META_EMBEDDED_SIGNUP"
        ) {
            runCatching {
                val token = channelToken(channel)
                val waba = channel[SocialChannelsTable.wabaId]
                if (!waba.isNullOrBlank()) {
                    httpClient.delete("https://graph.facebook.com/$graphApiVersion/$waba/subscribed_apps") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }
        }
        transaction {
            SocialChannelsTable.update({ SocialChannelsTable.id eq channelId }) {
                it[isActive] = false
                it[connectionStatus] = "DISCONNECTED"
                it[SocialChannelsTable.accessToken] = ""
                it[refreshToken] = null
                it[registrationPinEncrypted] = null
                it[disconnectedAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }
        return ApiResponse(true, message = "Channel disconnected and stored authorization removed")
    }

    // ── Conversation List ─────────────────────────────────────────────────────

    suspend fun getConversations(
        businessId: String,
        platform: String? = null,
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 30
    ): PagedResponse<ConversationResponse> {
        return transaction {
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
    }

    // ── Conversation Detail ───────────────────────────────────────────────────

    suspend fun getConversationDetail(businessId: String, conversationId: String): ConversationDetailResponse? {
        return transaction {
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
            SocialChannelsTable.select {
                SocialChannelsTable.id eq conv[SocialConversationsTable.channelId]
            }.firstOrNull()
        }
        val platform = conv[SocialConversationsTable.platform]
        val recipientId = conv[SocialConversationsTable.customerExternalId]
        val sendResult = if (channel == null) false else when (platform) {
            "WHATSAPP" -> sendWhatsAppMessage(
                channelToken(channel),
                channel[SocialChannelsTable.phoneNumberId] ?: channel[SocialChannelsTable.externalId],
                recipientId,
                req.content
            )
            "INSTAGRAM" -> sendInstagramDm(
                channelToken(channel),
                channel[SocialChannelsTable.externalId],
                recipientId,
                req.content
            )
            "FACEBOOK" -> sendFacebookMessage(
                channelToken(channel),
                channel[SocialChannelsTable.externalId],
                recipientId,
                req.content
            )
            "TIKTOK" -> sendTikTokDm(
                channelToken(channel),
                conv[SocialConversationsTable.externalConvId],
                req.content
            )
            else -> false
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
        val detail = getConversationDetail(businessId, req.conversationId)
            ?: return ApiResponse(false, message = "Conversation not found")

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
            "💳 *M-Pesa payment:*\nTutakutumia ombi salama la malipo kwa simu yako.\nKiasi: KES ${"%,.0f".format(req.amount)}"
        }

        val paymentMsg = buildString {
            append("Hujambo $customerName! 🛍️\n\n")
            append("*${req.description}*\n")
            append("💰 Jumla: *KES ${"%,.0f".format(req.amount)}*\n\n")
            if (mpesaPaybill != null) {
                append("$mpesaPaybill\n\n")
            }
            append("Unaweza pia kuchagua njia nyingine ya malipo kupitia biashara.\n\n")
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
        val result = orderService.create(businessId, orderReq, "social")
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
            val channel    = findWhatsAppChannelByWabaId(wabaId) ?: return@forEach

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

    private suspend fun sendWhatsAppMessage(token: String, phoneNumberId: String, to: String, text: String): Boolean {
        if (token.isBlank()) return false
        return try {
            val r = httpClient.post("https://graph.facebook.com/$graphApiVersion/$phoneNumberId/messages") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
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

    fun verifyMetaWebhookToken(token: String): Boolean {
        if (metaWebhookVerifyToken.isBlank() || token.isBlank()) return false
        return MessageDigest.isEqual(
            metaWebhookVerifyToken.toByteArray(Charsets.UTF_8),
            token.toByteArray(Charsets.UTF_8)
        )
    }

    fun verifyMetaWebhookSignature(rawBody: String, signature: String?): Boolean {
        if (metaAppSecret.isBlank() || signature.isNullOrBlank() || !signature.startsWith("sha256=")) return false
        val expected = signature.removePrefix("sha256=").lowercase()
        if (expected.length != 64 || expected.any { it !in "0123456789abcdef" }) return false
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(metaAppSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val actual = mac.doFinal(rawBody.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            actual.toByteArray(Charsets.UTF_8)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun graphGet(path: String, token: String, fields: String): Pair<Boolean, JsonObject> {
        val response = httpClient.get("https://graph.facebook.com/$graphApiVersion/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("fields", fields)
        }
        return response.status.isSuccess() to response.safeGraphJson()
    }

    private fun channelToken(channel: ResultRow): String {
        val stored = channel[SocialChannelsTable.accessToken]
        if (stored.isBlank()) return metaSystemUserToken
        if (!TokenCipher.isEncrypted(stored)) return stored
        return requireNotNull(tokenCipher) {
            "SOCIAL_TOKEN_ENCRYPTION_KEY is required to decrypt merchant credentials"
        }.decrypt(stored)
    }

    private suspend fun autoReplyIfNeeded(channel: ResultRow, convId: String, @Suppress("UNUSED_PARAMETER") inboundText: String) {
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
                    "WHATSAPP"  -> sendWhatsAppMessage(
                        channelToken(channel),
                        channel[SocialChannelsTable.phoneNumberId] ?: channel[SocialChannelsTable.externalId],
                        conv[SocialConversationsTable.customerExternalId],
                        reply
                    )
                    "INSTAGRAM" -> sendInstagramDm(channelToken(channel), channel[SocialChannelsTable.externalId], conv[SocialConversationsTable.customerExternalId], reply)
                    "FACEBOOK"  -> sendFacebookMessage(channelToken(channel), channel[SocialChannelsTable.externalId], conv[SocialConversationsTable.customerExternalId], reply)
                    "TIKTOK"    -> sendTikTokDm(channelToken(channel), conv[SocialConversationsTable.externalConvId], reply)
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

    private fun findWhatsAppChannelByWabaId(wabaId: String): ResultRow? = transaction {
        SocialChannelsTable.select {
            (SocialChannelsTable.platform eq "WHATSAPP") and
                ((SocialChannelsTable.wabaId eq wabaId) or
                    ((SocialChannelsTable.wabaId.isNull()) and (SocialChannelsTable.externalId eq wabaId))) and
                (SocialChannelsTable.isActive eq true)
        }.firstOrNull()
    }

    private fun upsertConversation(channel: ResultRow, platform: String, senderId: String, externalMsgId: String, senderName: String, phone: String?): String = transaction {
        val channelId  = channel[SocialChannelsTable.id]
        val businessId = channel[SocialChannelsTable.businessId]
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
        val token = if (this[SocialChannelsTable.onboardingMethod] == "META_EMBEDDED_SIGNUP") {
            ""
        } else {
            this[SocialChannelsTable.webhookVerifyToken]
        }
        return SocialChannelResponse(
            id                 = this[SocialChannelsTable.id],
            platform           = this[SocialChannelsTable.platform],
            channelName        = this[SocialChannelsTable.channelName],
            externalId         = this[SocialChannelsTable.externalId],
            phoneNumber        = this[SocialChannelsTable.phoneNumber],
            tenantId           = this[SocialChannelsTable.tenantId],
            wabaId             = this[SocialChannelsTable.wabaId],
            phoneNumberId      = this[SocialChannelsTable.phoneNumberId],
            metaBusinessId     = this[SocialChannelsTable.metaBusinessId],
            isActive           = this[SocialChannelsTable.isActive],
            connectionStatus   = this[SocialChannelsTable.connectionStatus],
            onboardingMethod   = this[SocialChannelsTable.onboardingMethod],
            lastVerifiedAt     = this[SocialChannelsTable.lastVerifiedAt]?.toString(),
            autoReplyEnabled   = this[SocialChannelsTable.autoReplyEnabled],
            aiPersonaPrompt    = this[SocialChannelsTable.aiPersonaPrompt],
            webhookVerifyToken = token,
            webhookUrl         = "https://api.biashara360.co.ke/v1/social/webhook/${this[SocialChannelsTable.platform].lowercase()}",
            unreadCount        = unread,
            createdAt          = this[SocialChannelsTable.createdAt].toString()
        )
    }
}

private suspend fun HttpResponse.safeGraphJson(): JsonObject {
    val body = bodyAsText()
    return runCatching { Json.parseToJsonElement(body).jsonObject }
        .getOrElse { buildJsonObject { put("message", "Invalid response from Meta") } }
}

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.graphErrorMessage(fallback: String): String =
    this["error"]?.jsonObject?.string("message")
        ?.take(240)
        ?.takeIf { it.isNotBlank() }
        ?: fallback

private fun String.isMetaId(): Boolean =
    length in 5..64 && all(Char::isDigit)
