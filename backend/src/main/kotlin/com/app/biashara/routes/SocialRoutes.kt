package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SocialService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlinx.serialization.json.Json

// ─── Social Commerce Routes ───────────────────────────────────────────────────
//
// Protected (JWT):
//   GET    /v1/social/channels                   list connected channels
//   POST   /v1/social/channels                   connect a channel
//   DELETE /v1/social/channels/{id}              disconnect channel
//   PATCH  /v1/social/channels/{id}/settings     update auto-reply / AI persona
//
//   GET    /v1/social/inbox                      unified inbox (all conversations)
//   GET    /v1/social/inbox/stats                inbox KPI stats
//   GET    /v1/social/conversations/{id}         conversation detail + messages
//   PATCH  /v1/social/conversations/{id}/status  update conversation status
//
//   POST   /v1/social/messages/send              send manual reply
//   POST   /v1/social/messages/ai-reply          get AI-generated reply suggestion
//   POST   /v1/social/messages/send-ai           send AI reply immediately
//
//   POST   /v1/social/orders                     create order from conversation
//   POST   /v1/social/payment-prompt             send payment request to customer
//
// Public (webhook endpoints — no JWT, verified by platform token):
//   GET    /v1/social/webhook/whatsapp           Meta webhook verification
//   POST   /v1/social/webhook/whatsapp           incoming WhatsApp messages
//   GET    /v1/social/webhook/instagram          Meta webhook verification
//   POST   /v1/social/webhook/instagram          incoming Instagram DMs + comments
//   GET    /v1/social/webhook/facebook           Meta webhook verification
//   POST   /v1/social/webhook/facebook           incoming Messenger + comments
//   POST   /v1/social/webhook/tiktok/{channelId} incoming TikTok DMs + comments

fun Route.socialRoutes() {
    val svc: SocialService by inject()

    route("/social") {

        get("/meta/configuration") {
            call.respond(ApiResponse(true, data = svc.getMetaOnboardingConfiguration()))
        }

        post("/meta/embedded-signup/complete") {
            if (!call.hasRole("ADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Merchant admin access required"))
                return@post
            }
            val result = svc.completeMetaEmbeddedSignup(
                call.businessId(),
                call.receive<MetaEmbeddedSignupRequest>()
            )
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        // ── Channel Management ─────────────────────────────────────────────

        route("/channels") {
            get {
                val businessId = call.businessId()
                val channels   = svc.getChannels(businessId)
                call.respond(ApiResponse(true, data = channels))
            }
            post {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Merchant admin access required"))
                    return@post
                }
                val businessId = call.businessId()
                val req        = call.receive<SocialChannelRequest>()
                if (req.platform.uppercase() !in listOf("WHATSAPP","INSTAGRAM","FACEBOOK")) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false,
                        message = "Platform must be WHATSAPP, INSTAGRAM or FACEBOOK. TikTok messaging is not available yet."))
                    return@post
                }
                if (req.platform.uppercase() != "WHATSAPP" && req.accessToken.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Access token required"))
                    return@post
                }
                val result = svc.connectChannel(businessId, req)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }
            delete("/{id}") {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Merchant admin access required"))
                    return@delete
                }
                val businessId = call.businessId()
                val id         = call.parameters["id"]!!
                val result     = svc.disconnectChannel(businessId, id)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
            post("/{id}/verify") {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Merchant admin access required"))
                    return@post
                }
                val result = svc.verifyChannelConnection(call.businessId(), call.parameters["id"]!!)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
            patch("/{id}/settings") {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Merchant admin access required"))
                    return@patch
                }
                val businessId = call.businessId()
                val id         = call.parameters["id"]!!
                val req        = call.receive<SocialChannelRequest>()
                val result     = svc.updateChannelSettings(businessId, id, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }

        // ── Inbox ──────────────────────────────────────────────────────────

        get("/inbox") {
            val businessId = call.businessId()
            val platform   = call.request.queryParameters["platform"]
            val status     = call.request.queryParameters["status"]
            val page       = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val convs      = svc.getConversations(businessId, platform, status, page)
            call.respond(ApiResponse(true, data = convs))
        }

        get("/inbox/stats") {
            val businessId = call.businessId()
            val stats      = svc.getInboxStats(businessId)
            call.respond(ApiResponse(true, data = stats))
        }

        // ── Conversations ──────────────────────────────────────────────────

        route("/conversations/{id}") {
            get {
                val businessId = call.businessId()
                val id         = call.parameters["id"]!!
                val detail     = svc.getConversationDetail(businessId, id)
                if (detail == null) call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Conversation not found"))
                else call.respond(ApiResponse(true, data = detail))
            }
            patch("/status") {
                val businessId = call.businessId()
                val id         = call.parameters["id"]!!
                val body       = call.receive<Map<String, String>>()
                val newStatus  = body["status"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "status required"))
                    return@patch
                }
                val result = svc.updateConversationStatus(businessId, id, newStatus)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }

        // ── Messages ──────────────────────────────────────────────────────

        post("/messages/send") {
            val businessId = call.businessId()
            val req        = call.receive<SendMessageRequest>()
            val result     = svc.sendMessage(businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        post("/messages/ai-reply") {
            val businessId = call.businessId()
            val req        = call.receive<AiReplyRequest>()
            val result     = svc.generateAiReply(businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        post("/messages/send-ai") {
            val businessId = call.businessId()
            val req        = call.receive<AiReplyRequest>()
            val aiResult   = svc.generateAiReply(businessId, req)
            if (!aiResult.success || aiResult.data == null) {
                call.respond(HttpStatusCode.BadRequest, aiResult)
                return@post
            }
            val sendResult = svc.sendMessage(businessId, SendMessageRequest(
                conversationId = req.conversationId,
                content        = aiResult.data.suggestedReply
            ))
            call.respond(if (sendResult.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, sendResult)
        }

        // ── Orders & Payments ──────────────────────────────────────────────

        post("/orders") {
            val businessId = call.businessId()
            val req        = call.receive<CreateSocialOrderRequest>()
            if (req.items.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Order must have at least one item"))
                return@post
            }
            val result = svc.createSocialOrder(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        post("/payment-prompt") {
            val businessId = call.businessId()
            val req        = call.receive<SendPaymentPromptRequest>()
            if (req.amount <= 0) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Amount must be positive"))
                return@post
            }
            val result = svc.sendPaymentPrompt(businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }
    }
}

// ─── Public Webhook Routes (no JWT; Meta signatures are mandatory) ───────────

fun Route.socialWebhookRoutes() {
    val svc: SocialService by inject()

    route("/social/webhook") {
        listOf("whatsapp", "instagram", "facebook").forEach { platform ->
            get("/$platform") {
                val mode = call.request.queryParameters["hub.mode"]
                val token = call.request.queryParameters["hub.verify_token"].orEmpty()
                val challenge = call.request.queryParameters["hub.challenge"].orEmpty()
                if (mode == "subscribe" && challenge.isNotBlank() && svc.verifyMetaWebhookToken(token)) {
                    call.respondText(challenge, ContentType.Text.Plain, HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Webhook verification failed"))
                }
            }
            post("/$platform") {
                val rawBody = call.receiveText()
                if (!svc.verifyMetaWebhookSignature(rawBody, call.request.headers["x-hub-signature-256"])) {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(false, message = "Invalid webhook signature"))
                    return@post
                }
                val payload = runCatching {
                    Json { ignoreUnknownKeys = true; isLenient = false }
                        .decodeFromString<MetaWebhookPayload>(rawBody)
                }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Invalid webhook payload"))
                    return@post
                }
                svc.processMetaWebhook(payload)
                call.respond(HttpStatusCode.OK, mapOf("received" to true))
            }
        }

        post("/tiktok/{channelId}") {
            call.respond(
                HttpStatusCode.NotImplemented,
                ApiResponse<Unit>(false, message = "TikTok webhook signing is not configured")
            )
        }
    }
}
