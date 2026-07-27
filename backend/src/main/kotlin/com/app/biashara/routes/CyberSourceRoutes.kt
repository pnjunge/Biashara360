package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import org.koin.ktor.ext.inject

// ─── CyberSource Public Routes (no JWT required) ──────────────────────────────
// GET /v1/payments/card/capture-context — returns Flex JWT for Unified Checkout widget

fun Route.cyberSourcePublicRoutes() {
    val csService: CyberSourcePaymentService by inject()
    val orderService: OrderService by inject()

    route("/payments/card") {
        /**
         * GET /v1/payments/card/capture-context?origin=https://yoursite.com
         *
         * Returns a short-lived Flex capture context JWT.
         * The web frontend passes this JWT to the Unified Checkout widget to
         * initialize the PCI-compliant hosted card entry fields.
         * No auth required — called before the customer logs in.
         */
        rateLimit(RateLimitName("public-payment-limiter")) {
        get("/capture-context") {
            val origin = call.request.queryParameters["origin"]
                ?: call.request.headers["Origin"]
                ?: "https://biashara360.co.ke"
            val rawBusinessId = call.request.queryParameters["businessId"]
            // Validate UUID format to reject obviously invalid values; an unrecognised
            // but well-formed ID will simply fall back to the global config in the service.
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            val businessId = rawBusinessId?.takeIf { uuidRegex.matches(it) }
            val allowedOrigins = call.application.environment.config.propertyOrNull("payments.allowedOrigins")
                ?.getList()
                ?: listOf("https://app.biashara360.co.ke", "https://biashara360.co.ke")
            if (origin !in allowedOrigins || businessId == null || !csService.hasTenantConfiguration(businessId)) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Invalid checkout origin or business"))
                return@get
            }
            val jwt = csService.getCaptureContext(origin, businessId)
            if (jwt != null) {
                call.respond(ApiResponse(true, data = mapOf("captureContextJwt" to jwt)))
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiResponse<Unit>(false, message = "Could not generate capture context from CyberSource")
                )
            }
        }

        get("/public-order") {
            val orderId = call.request.queryParameters["orderId"]
            val businessId = call.request.queryParameters["businessId"]
            if (orderId.isNullOrBlank() || businessId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Missing orderId or businessId"))
                return@get
            }
            val order = orderService.getById(orderId, businessId)
            if (order != null) {
                call.respond(ApiResponse(true, data = order))
            } else {
                val now = kotlinx.datetime.Clock.System.now().toString()
                val adHoc = OrderResponse(
                    id = orderId,
                    orderNumber = orderId,
                    businessId = businessId,
                    customerId = null,
                    customerName = "",
                    customerPhone = "",
                    deliveryLocation = "",
                    items = emptyList(),
                    paymentStatus = "PENDING",
                    deliveryStatus = "PENDING",
                    paymentMethod = "CARD",
                    mpesaTransactionCode = null,
                    subtotal = 0.0,
                    notes = "Card Payment Link",
                    createdAt = now,
                    updatedAt = now
                )
                call.respond(ApiResponse(true, data = adHoc))
            }
        }

        /**
         * POST /v1/payments/card/guest-charge
         *
         * Guest charge for shared payment links. Accepts businessId in the request.
         */
        post("/guest-charge") {
            val req = call.receive<CsGuestChargeRequest>()
            if (req.businessId.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "businessId is required")
                )
                return@post
            }
            if (req.transientToken.isNullOrBlank() && req.cardNumber.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "A CyberSource transient token or card number is required")
                )
                return@post
            }
            val order = orderService.getById(req.orderId, req.businessId)
            if (order != null) {
                if (order.paymentStatus == "PAID") {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "This order has already been paid"))
                    return@post
                }
                if (kotlin.math.abs(order.subtotal - req.amount) > 0.001) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Invalid payment order amount"))
                    return@post
                }
            }

            val chargeReq = CsChargeRequest(
                orderId = req.orderId,
                amount = req.amount,
                currency = req.currency,
                transientToken = req.transientToken,
                cardNumber = req.cardNumber,
                cardExpiryMonth = req.cardExpiryMonth,
                cardExpiryYear = req.cardExpiryYear,
                cardCvv = req.cardCvv,
                cardholderName = req.cardholderName,
                billingEmail = req.billingEmail,
                billingPhone = req.billingPhone,
                saveCard = false,
                savedCardId = null,
                captureImmediately = true
            )
            val result = csService.charge(req.businessId, chargeReq)
            val success = result.status in listOf("AUTHORIZED", "CAPTURED")
            if (success) {
                if (order != null) {
                    orderService.updatePaymentStatus(
                        req.orderId,
                        req.businessId,
                        UpdatePaymentStatusRequest("PAID", result.transactionId)
                    )
                } else {
                    orderService.create(
                        req.businessId,
                        CreateOrderRequest(
                            clientReference = req.orderId,
                            customerId = null,
                            customerName = req.cardholderName?.ifBlank { "Card Customer" } ?: "Card Customer",
                            customerPhone = req.billingPhone ?: "",
                            deliveryLocation = "",
                            items = emptyList(),
                            paymentMethod = "CARD",
                            paymentStatus = "PAID",
                            deliveryStatus = "DELIVERED",
                            notes = "Card Payment Link (${result.transactionId})"
                        )
                    )
                }
            }
            val status = if (success) HttpStatusCode.OK else HttpStatusCode.PaymentRequired
            call.respond(status, ApiResponse(success, data = result, message = result.errorMessage ?: ""))
        }
        }
    }
}

// ─── CyberSource Protected Routes (JWT required) ──────────────────────────────

fun Route.cyberSourceRoutes() {
    val csService: CyberSourcePaymentService by inject()
    val orderService: OrderService by inject()

    route("/payments/card") {

        /**
         * POST /v1/payments/card/generate-link
         *
         * Generates a hosted payment link to send to a customer via Email, WhatsApp, or SMS.
         */
        post("/generate-link") {
            val businessId = call.businessId()
            val req = call.receive<CsPaymentLinkRequest>()
            if (req.orderId.isBlank() || req.amount <= 0) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Valid orderId and positive amount are required"))
                return@post
            }
            val origin = call.request.headers["Origin"] ?: "https://app.biashara360.co.ke"
            val response = csService.generatePaymentLink(businessId, req, origin)
            call.respond(ApiResponse(true, data = response, message = "Payment link generated successfully"))
        }

        /**
         * POST /v1/payments/card/charge
         *
         * Charge a card payment. Accepts:
         *   - flexToken: transient token from Unified Checkout widget (preferred, no PCI scope)
         *   - savedCardId: our DB id of a previously tokenized card
         *   - cardNumber/cardExpiryMonth/cardExpiryYear/cardCvv: raw card (only for PCI-certified integrations)
         *
         * Set captureImmediately=true for immediate settlement (retail/COD confirmation).
         * Set captureImmediately=false to authorize only, then call /capture when order ships.
         * Set saveCard=true to tokenize the card in CyberSource TMS for future charges.
         *
         * Response statuses: AUTHORIZED | CAPTURED | DECLINED | ERROR
         */
        post("/charge") {
            val businessId = call.businessId()
            val req = call.receive<CsChargeRequest>()

            if (req.transientToken == null && req.savedCardId == null && req.cardNumber == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Provide flexToken, savedCardId, or card number")
                )
                return@post
            }

            val result = csService.charge(businessId, req)
            val success = result.status in listOf("AUTHORIZED", "CAPTURED")
            if (success && !req.orderId.isNullOrBlank()) {
                orderService.updatePaymentStatus(
                    req.orderId,
                    businessId,
                    UpdatePaymentStatusRequest("PAID", result.transactionId)
                )
            }
            val status = if (success) HttpStatusCode.OK else HttpStatusCode.PaymentRequired
            call.respond(status, ApiResponse(success, data = result, message = result.errorMessage ?: ""))
        }

        /**
         * POST /v1/payments/card/capture
         *
         * Capture a previously authorized (auth-only) payment.
         * Call this when the order is shipped / fulfilled.
         * Body: { csTransactionId, amount? }
         */
        post("/capture") {
            val businessId = call.businessId()
            val req = call.receive<CsCaptureRouteRequest>()
            val result = csService.capture(businessId, req)
            call.respond(ApiResponse(result.status == "CAPTURED", data = result, message = result.errorMessage ?: ""))
        }

        /**
         * POST /v1/payments/card/refund
         *
         * Refund a captured/settled payment (partial or full).
         * Body: { csTransactionId, amount, reason? }
         */
        post("/refund") {
            val businessId = call.businessId()
            val req = call.receive<CsRefundRouteRequest>()
            val result = csService.refund(businessId, req)
            call.respond(ApiResponse(result.status == "REFUNDED", data = result, message = result.errorMessage ?: ""))
        }

        /**
         * POST /v1/payments/card/void
         *
         * Void an authorization before it is captured/settled.
         * Only valid while order hasn't shipped yet.
         * Body: { csTransactionId }
         */
        post("/void") {
            val businessId = call.businessId()
            val req = call.receive<CsVoidRouteRequest>()
            val result = csService.void(businessId, req)
            call.respond(ApiResponse(result.status == "VOIDED", data = result, message = result.errorMessage ?: ""))
        }

        /**
         * GET /v1/payments/card/transactions
         *
         * Returns full CyberSource card transaction history for the business.
         * Includes all auths, captures, refunds, voids, and errors.
         */
        get("/transactions") {
            val businessId = call.businessId()
            val txns = csService.getTransactions(businessId)
            call.respond(ApiResponse(true, data = txns))
        }

        /**
         * GET /v1/payments/card/saved-cards?customerId=xxx
         *
         * Returns tokenized saved cards stored in CyberSource TMS for this business.
         * Use customerId to filter to a specific customer's cards.
         */
        get("/saved-cards") {
            val businessId = call.businessId()
            val customerId = call.request.queryParameters["customerId"]
            val cards = csService.getSavedCards(businessId, customerId)
            call.respond(ApiResponse(true, data = cards))
        }

        /**
         * DELETE /v1/payments/card/saved-cards/{id}
         *
         * Remove a saved card token from the system.
         */
        // delete("/saved-cards/{id}") {
        //     val businessId = call.businessId()
        //     val cardId = call.parameters["id"]!!
        //     csService.deleteSavedCard(businessId, cardId)
        //     call.respond(ApiResponse<Unit>(true, message = "Card removed"))
        // }
    }
}
