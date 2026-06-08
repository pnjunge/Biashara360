package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SocialService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

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

        // ── Channel Management ─────────────────────────────────────────────

        route("/channels") {
            get {
                val businessId = call.businessId()
                val channels   = svc.getChannels(businessId)
                call.respond(ApiResponse(true, data = channels))
            }
            post {
                val businessId = call.businessId()
                val req        = call.receive<SocialChannelRequest>()
                if (req.platform.uppercase() !in listOf("WHATSAPP","INSTAGRAM","FACEBOOK","TIKTOK")) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false,
                        message = "Platform must be WHATSAPP, INSTAGRAM, FACEBOOK or TIKTOK"))
                    return@post
                }
                if (req.accessToken.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Access token required"))
                    return@post
                }
                val result = svc.connectChannel(businessId, req)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }
            delete("/{id}") {
                val businessId = call.businessId()
                val id         = call.parameters["id"]!!
                val result     = svc.disconnectChannel(businessId, id)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
            patch("/{id}/settings") {
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

// ─── Public Webhook Routes (no JWT) ──────────────────────────────────────────

fun Route.socialWebhookRoutes() {
    val svc: SocialService by inject()

    route("/social/webhook") {

        // ── WhatsApp ───────────────────────────────────────────────────────

        get("/whatsapp") {
            val mode      = call.request.queryParameters["hub.mode"]
            val token     = call.request.queryParameters["hub.verify_token"]
            val challenge = call.request.queryParameters["hub.challenge"]
            if (mode == "subscribe" && svc.verifyWebhookToken("WHATSAPP", token ?: "")) {
                call.respondText(challenge ?: "", ContentType.Text.Plain)
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid verify token")
            }
        }
        post("/whatsapp") {
            val payload = call.receive<MetaWebhookPayload>()
            svc.processMetaWebhook(payload)
            call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
        }

        // ── Instagram ──────────────────────────────────────────────────────

        get("/instagram") {
            val mode      = call.request.queryParameters["hub.mode"]
            val token     = call.request.queryParameters["hub.verify_token"]
            val challenge = call.request.queryParameters["hub.challenge"]
            if (mode == "subscribe" && svc.verifyWebhookToken("INSTAGRAM", token ?: "")) {
                call.respondText(challenge ?: "", ContentType.Text.Plain)
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid verify token")
            }
        }
        post("/instagram") {
            val payload = call.receive<MetaWebhookPayload>()
            svc.processMetaWebhook(payload)
            call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
        }

        // ── Facebook ───────────────────────────────────────────────────────

        get("/facebook") {
            val mode      = call.request.queryParameters["hub.mode"]
            val token     = call.request.queryParameters["hub.verify_token"]
            val challenge = call.request.queryParameters["hub.challenge"]
            if (mode == "subscribe" && svc.verifyWebhookToken("FACEBOOK", token ?: "")) {
                call.respondText(challenge ?: "", ContentType.Text.Plain)
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid verify token")
            }
        }
        post("/facebook") {
            val payload = call.receive<MetaWebhookPayload>()
            svc.processMetaWebhook(payload)
            call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
        }

        // ── TikTok ─────────────────────────────────────────────────────────

        post("/tiktok/{channelId}") {
            val channelId = call.parameters["channelId"]!!
            val payload   = call.receive<TikTokWebhookPayload>()
            svc.processTikTokWebhook(channelId, payload)
            call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
        }
    }
}
