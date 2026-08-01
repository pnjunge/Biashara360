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

fun Route.cyberSourcePublicRoutes() {
    val orderService: OrderService by inject()
    val saService: SecureAcceptanceService by inject()

    route("/public/payments/card") {
        get("/public-order") {
            val orderId    = call.request.queryParameters["orderId"]
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
                    id = orderId, orderNumber = orderId, businessId = businessId,
                    customerId = null, customerName = "", customerPhone = "",
                    deliveryLocation = "", items = emptyList(),
                    paymentStatus = "PENDING", deliveryStatus = "PENDING",
                    paymentMethod = "CARD", mpesaTransactionCode = null,
                    subtotal = 0.0, notes = "Card Payment Link",
                    createdAt = now, updatedAt = now
                )
                call.respond(ApiResponse(true, data = adHoc))
            }
        }

        post("/sa-initiate") {
            val req = call.receive<SaInitiateRequest>()
            if (req.businessId.isBlank() || req.orderId.isBlank() || req.amount <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "businessId, orderId, and a positive amount are required")
                )
                return@post
            }
            val order = orderService.getById(req.orderId, req.businessId)
            if (order?.paymentStatus == "PAID") {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "This order has already been paid")
                )
                return@post
            }

            val result = saService.buildSaForm(
                SaFormRequest(
                    businessId    = req.businessId,
                    orderId       = req.orderId,
                    amount        = req.amount,
                    customerName  = req.customerName,
                    customerEmail = req.customerEmail,
                    customerPhone = req.customerPhone
                )
            )

            if (!result.success) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiResponse<Unit>(false, message = result.error ?: "Could not build Secure Acceptance form")
                )
                return@post
            }

            call.respond(
                ApiResponse(
                    true,
                    data = SaInitiateResponse(actionUrl = result.actionUrl, fields = result.fields)
                )
            )
        }

        post("/sa-notify") {
            val params = call.receiveParameters()
            val fields = params.entries().associate { entry -> entry.key to (entry.value.firstOrNull() ?: "") }

            val notifyResult = saService.verifyAndProcess(fields)

            if (!notifyResult.verified) {
                call.respond(HttpStatusCode.BadRequest, "Signature verification failed")
                return@post
            }
            call.respond(HttpStatusCode.OK, "")
        }

        get("/sa-return") {
            val decision   = call.request.queryParameters["decision"] ?: "ERROR"
            val orderId    = call.request.queryParameters["req_reference_number"] ?: ""
            val businessId = call.request.queryParameters["req_merchant_defined_data1"] ?: ""
            val csTransId  = call.request.queryParameters["transaction_id"] ?: ""

            val webBase = "https://app.biashara360.co.ke"

            val redirectUrl = when (decision) {
                "ACCEPT" -> "$webBase/pay/card?status=success&orderId=$orderId&businessId=$businessId&txnId=$csTransId"
                "CANCEL" -> "$webBase/pay/card?status=cancelled&orderId=$orderId&businessId=$businessId"
                else     -> "$webBase/pay/card?status=declined&orderId=$orderId&businessId=$businessId"
            }
            call.respondRedirect(redirectUrl, permanent = false)
        }

        post("/sa-return") {
            val params     = call.receiveParameters()
            val decision   = params["decision"] ?: "ERROR"
            val orderId    = params["req_reference_number"] ?: params["req_merchant_defined_data2"] ?: ""
            val businessId = params["req_merchant_defined_data1"] ?: ""
            val csTransId  = params["transaction_id"] ?: ""

            val fields = params.entries().associate { entry -> entry.key to (entry.value.firstOrNull() ?: "") }
            saService.verifyAndProcess(fields)

            val webBase = "https://app.biashara360.co.ke"
            val redirectUrl = when (decision) {
                "ACCEPT" -> "$webBase/pay/card?status=success&orderId=$orderId&businessId=$businessId&txnId=$csTransId"
                "CANCEL" -> "$webBase/pay/card?status=cancelled&orderId=$orderId&businessId=$businessId"
                else     -> "$webBase/pay/card?status=declined&orderId=$orderId&businessId=$businessId"
            }
            call.respondRedirect(redirectUrl, permanent = false)
        }
    }

    // Direct path mappings for legacy unauthenticated /payments/card routes
    get("/payments/card/public-order") {
        val orderId    = call.request.queryParameters["orderId"]
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
                id = orderId, orderNumber = orderId, businessId = businessId,
                customerId = null, customerName = "", customerPhone = "",
                deliveryLocation = "", items = emptyList(),
                paymentStatus = "PENDING", deliveryStatus = "PENDING",
                paymentMethod = "CARD", mpesaTransactionCode = null,
                subtotal = 0.0, notes = "Card Payment Link",
                createdAt = now, updatedAt = now
            )
            call.respond(ApiResponse(true, data = adHoc))
        }
    }

    post("/payments/card/sa-initiate") {
        val req = call.receive<SaInitiateRequest>()
        if (req.businessId.isBlank() || req.orderId.isBlank() || req.amount <= 0) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(false, message = "businessId, orderId, and a positive amount are required")
            )
            return@post
        }
        val order = orderService.getById(req.orderId, req.businessId)
        if (order?.paymentStatus == "PAID") {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(false, message = "This order has already been paid")
            )
            return@post
        }

        val result = saService.buildSaForm(
            SaFormRequest(
                businessId    = req.businessId,
                orderId       = req.orderId,
                amount        = req.amount,
                customerName  = req.customerName,
                customerEmail = req.customerEmail,
                customerPhone = req.customerPhone
            )
        )

        if (!result.success) {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiResponse<Unit>(false, message = result.error ?: "Could not build Secure Acceptance form")
            )
            return@post
        }

        call.respond(
            ApiResponse(
                true,
                data = SaInitiateResponse(actionUrl = result.actionUrl, fields = result.fields)
            )
        )
    }

    post("/payments/card/sa-notify") {
        val params = call.receiveParameters()
        val fields = params.entries().associate { entry -> entry.key to (entry.value.firstOrNull() ?: "") }

        val notifyResult = saService.verifyAndProcess(fields)

        if (!notifyResult.verified) {
            call.respond(HttpStatusCode.BadRequest, "Signature verification failed")
            return@post
        }
        call.respond(HttpStatusCode.OK, "")
    }

    get("/payments/card/sa-return") {
        val decision   = call.request.queryParameters["decision"] ?: "ERROR"
        val orderId    = call.request.queryParameters["req_reference_number"] ?: ""
        val businessId = call.request.queryParameters["req_merchant_defined_data1"] ?: ""
        val csTransId  = call.request.queryParameters["transaction_id"] ?: ""

        val webBase = "https://app.biashara360.co.ke"

        val redirectUrl = when (decision) {
            "ACCEPT" -> "$webBase/pay/card?status=success&orderId=$orderId&businessId=$businessId&txnId=$csTransId"
            "CANCEL" -> "$webBase/pay/card?status=cancelled&orderId=$orderId&businessId=$businessId"
            else     -> "$webBase/pay/card?status=declined&orderId=$orderId&businessId=$businessId"
        }
        call.respondRedirect(redirectUrl, permanent = false)
    }

    post("/payments/card/sa-return") {
        val params     = call.receiveParameters()
        val decision   = params["decision"] ?: "ERROR"
        val orderId    = params["req_reference_number"] ?: params["req_merchant_defined_data2"] ?: ""
        val businessId = params["req_merchant_defined_data1"] ?: ""
        val csTransId  = params["transaction_id"] ?: ""

        val fields = params.entries().associate { entry -> entry.key to (entry.value.firstOrNull() ?: "") }
        saService.verifyAndProcess(fields)

        val webBase = "https://app.biashara360.co.ke"
        val redirectUrl = when (decision) {
            "ACCEPT" -> "$webBase/pay/card?status=success&orderId=$orderId&businessId=$businessId&txnId=$csTransId"
            "CANCEL" -> "$webBase/pay/card?status=cancelled&orderId=$orderId&businessId=$businessId"
            else     -> "$webBase/pay/card?status=declined&orderId=$orderId&businessId=$businessId"
        }
        call.respondRedirect(redirectUrl, permanent = false)
    }
}


// ─── CyberSource Protected Routes (JWT required) ──────────────────────────────

fun Route.cyberSourceRoutes() {
    val csService: CyberSourcePaymentService by inject()
    val orderService: OrderService by inject()

    route("/payments/card/manage") {

        /**
         * POST /v1/payments/card/manage/generate-link
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
         * POST /v1/payments/card/manage/charge
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
         * POST /v1/payments/card/manage/capture
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
         * POST /v1/payments/card/manage/refund
         *
         * Refund a captured/settled payment (partial or full).
         * Body: { csTransactionId, amount, reason? }
         */
        post("/refund") {
            if(!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Manager approval is required for refunds"))
            val businessId = call.businessId()
            val req = call.receive<CsRefundRouteRequest>()
            val result = csService.refund(businessId, req)
            call.respond(ApiResponse(result.status == "REFUNDED", data = result, message = result.errorMessage ?: ""))
        }

        /**
         * POST /v1/payments/card/manage/void
         *
         * Void an authorization before it is captured/settled.
         * Only valid while order hasn't shipped yet.
         * Body: { csTransactionId }
         */
        post("/void") {
            if(!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Manager approval is required for voids"))
            val businessId = call.businessId()
            val req = call.receive<CsVoidRouteRequest>()
            val result = csService.void(businessId, req)
            call.respond(ApiResponse(result.status == "VOIDED", data = result, message = result.errorMessage ?: ""))
        }

        /**
         * GET /v1/payments/card/manage/transactions
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
         * GET /v1/payments/card/manage/saved-cards?customerId=xxx
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
         * DELETE /v1/payments/card/manage/saved-cards/{id}
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
