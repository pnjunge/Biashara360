package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

// ─── CyberSource Public Routes (no JWT required) ──────────────────────────────
// GET /v1/payments/card/capture-context — returns Flex JWT for Unified Checkout widget

fun Route.cyberSourcePublicRoutes() {
    val csService: CyberSourcePaymentService by inject()

    route("/payments/card") {
        /**
         * GET /v1/payments/card/capture-context?origin=https://yoursite.com
         *
         * Returns a short-lived Flex capture context JWT.
         * The web frontend passes this JWT to the Unified Checkout widget to
         * initialize the PCI-compliant hosted card entry fields.
         * No auth required — called before the customer logs in.
         */
        get("/capture-context") {
            val origin = call.request.queryParameters["origin"]
                ?: call.request.headers["Origin"]
                ?: "https://biashara360.co.ke"
            val rawBusinessId = call.request.queryParameters["businessId"]
            // Validate UUID format to reject obviously invalid values; an unrecognised
            // but well-formed ID will simply fall back to the global config in the service.
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            val businessId = rawBusinessId?.takeIf { uuidRegex.matches(it) }
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
    }
}

// ─── CyberSource Protected Routes (JWT required) ──────────────────────────────

fun Route.cyberSourceRoutes() {
    val csService: CyberSourcePaymentService by inject()

    route("/payments/card") {

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
