package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.EtimsService
import com.app.biashara.services.KraService
import com.app.biashara.services.OrderService
import com.app.biashara.services.TaxService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

// ─── KRA iTax Routes ──────────────────────────────────────────────────────────
// All endpoints are under /v1/kra and require JWT auth
//
//  Profile
//  GET    /v1/kra/profile                          get KRA profile (PIN, eTIMS SDC)
//  POST   /v1/kra/profile                          save/update KRA profile
//
//  Compliance
//  GET    /v1/kra/compliance                       compliance score + pending returns
//
//  eTIMS Invoice Transmission
//  POST   /v1/kra/etims/transmit                   transmit one order to KRA eTIMS
//  POST   /v1/kra/etims/retry                      retry all failed/pending transmissions
//  GET    /v1/kra/etims/history                    transmission history
//  GET    /v1/kra/etims/pending                    invoices awaiting transmission
//  POST   /v1/kra/etims/device/init                initialise eTIMS virtual device (SDC)
//
//  Tax Returns
//  POST   /v1/kra/returns/vat3                     generate VAT3 return for a period
//  POST   /v1/kra/returns/tot                      generate TOT return for a period
//  POST   /v1/kra/returns/wht                      generate WHT return for a period
//  PATCH  /v1/kra/returns/{id}/submitted           mark a return as submitted with ack no.
//
//  CSV Export (for manual iTax upload)
//  POST   /v1/kra/export/csv                       generate & download KRA-format CSV

fun Route.kraRoutes() {
    val kraService: KraService by inject()
    val etimsService: EtimsService by inject()
    val orderService: OrderService by inject()
    val taxService: TaxService by inject()

    route("/kra") {

        // ── KRA Profile ───────────────────────────────────────────────────────

        route("/profile") {
            get {
                val businessId = call.businessId()
                val profile    = kraService.getProfile(businessId)
                if (profile == null)
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "KRA profile not configured. Add your KRA PIN to get started."))
                else
                    call.respond(ApiResponse(true, data = profile))
            }

            post {
                val businessId = call.businessId()
                val req        = call.receive<KraProfileRequest>()
                if (req.pin.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "KRA PIN is required"))
                    return@post
                }
                // Validate PIN format: letter + 9 digits + letter (e.g. P051234567X)
                val pinRegex = Regex("^[A-Z][0-9]{9}[A-Z]$")
                if (!pinRegex.matches(req.pin.uppercase().trim())) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Invalid KRA PIN format. Expected format: P051234567X"))
                    return@post
                }
                val result = kraService.saveProfile(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }

        // ── Compliance Dashboard ──────────────────────────────────────────────

        get("/compliance") {
            val businessId = call.businessId()
            val result     = kraService.getComplianceStatus(businessId)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        // ── eTIMS Endpoints ───────────────────────────────────────────────────

        route("/etims") {

            // Transmit a single order to KRA eTIMS in real-time
            post("/transmit") {
                val businessId = call.businessId()
                val req        = call.receive<EtimsInvoiceRequest>()

                val profile = kraService.getProfile(businessId)
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false,
                            message = "KRA profile not configured. Set your KRA PIN and eTIMS SDC ID first."))
                        return@post
                    }

                if (profile.etimsSdcId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false,
                        message = "eTIMS SDC ID not configured. Register a virtual device on the KRA eTIMS portal first."))
                    return@post
                }

                // Load the order
                val order = orderService.getById(req.orderId, businessId)
                    ?: run {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Order not found"))
                        return@post
                    }

                // Load tax lines
                val taxLines = taxService.getOrderTaxLines(req.orderId)

                // Create pending DB record first
                val internalId = java.util.UUID.randomUUID().toString()

                // Transmit to KRA
                val result = etimsService.transmitInvoice(profile, internalId, order, taxLines, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity, result)
            }

            // Retry all PENDING / ERROR invoices (run as a scheduled job or manually)
            post("/retry") {
                val businessId = call.businessId()
                val profile    = kraService.getProfile(businessId)
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "KRA profile not configured"))
                        return@post
                    }
                val pending = etimsService.getPendingTransmissions(businessId)
                var retried = 0; var succeeded = 0
                pending.forEach { inv ->
                    val order = orderService.getById(inv.orderId, businessId) ?: return@forEach
                    val taxLines = taxService.getOrderTaxLines(inv.orderId)
                    val req      = EtimsInvoiceRequest(orderId = inv.orderId, invoiceNumber = inv.invoiceNumber)
                    val r        = etimsService.transmitInvoice(profile, inv.internalId, order, taxLines, req)
                    retried++; if (r.success) succeeded++
                }
                call.respond(ApiResponse<Unit>(true, message = "Retried $retried invoices. $succeeded succeeded."))
            }

            // Transmission history
            get("/history") {
                val businessId = call.businessId()
                val limit      = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val history    = etimsService.getTransmissionHistory(businessId, limit)
                call.respond(ApiResponse(true, data = history))
            }

            // Invoices awaiting transmission
            get("/pending") {
                val businessId = call.businessId()
                val pending    = etimsService.getPendingTransmissions(businessId)
                call.respond(ApiResponse(true, data = pending))
            }

            // Initialise eTIMS virtual device / SDC
            post("/device/init") {
                val businessId = call.businessId()
                val req        = call.receive<EtimsDeviceInitRequest>()
                val profile    = kraService.getProfile(businessId)
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Save KRA profile (PIN) before initialising device"))
                        return@post
                    }
                val result = etimsService.initDevice(profile, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }

        // ── Tax Returns ───────────────────────────────────────────────────────

        route("/returns") {

            post("/vat3") {
                val businessId = call.businessId()
                val req        = call.receive<Vat3ReturnRequest>()
                if (req.periodMonth !in 1..12) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Month must be 1–12"))
                    return@post
                }
                val result = kraService.generateVat3Return(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }

            post("/tot") {
                val businessId = call.businessId()
                val req        = call.receive<TotReturnRequest>()
                val result     = kraService.generateTotReturn(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }

            post("/wht") {
                val businessId = call.businessId()
                val req        = call.receive<WhtReturnRequest>()
                val result     = kraService.generateWhtReturn(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }

            patch("/{id}/submitted") {
                val businessId = call.businessId()
                val returnId   = call.parameters["id"]!!
                val ackNo      = call.request.queryParameters["ackNo"]
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ackNo query param required"))
                        return@patch
                    }
                val result = kraService.markReturnSubmitted(businessId, returnId, ackNo)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }

        // ── CSV Export for manual iTax upload ─────────────────────────────────

        post("/export/csv") {
            val businessId = call.businessId()
            val req        = call.receive<CsvExportRequest>()
            if (req.returnType !in listOf("VAT3","TOT","WHT","ETIMS_INVOICES")) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "returnType must be VAT3, TOT, WHT or ETIMS_INVOICES"))
                return@post
            }
            val result = kraService.generateCsv(businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }
    }
}
