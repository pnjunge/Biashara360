package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.TaxService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// ─── Tax Routes ───────────────────────────────────────────────────────────────
// All endpoints are under /v1/tax and require JWT auth
//
//  Tax Rates
//  GET    /v1/tax/rates                    list all tax rates
//  POST   /v1/tax/rates                    create tax rate
//  PUT    /v1/tax/rates/{id}               update tax rate
//  PATCH  /v1/tax/rates/{id}/toggle        enable/disable rate
//  DELETE /v1/tax/rates/{id}               delete rate
//  POST   /v1/tax/rates/seed-defaults      seed Kenya defaults (VAT, TOT, WHT, Excise)
//
//  Tax Calculation (utility)
//  POST   /v1/tax/calculate                calculate tax for an amount
//
//  Order Tax Lines
//  GET    /v1/tax/orders/{orderId}         get tax breakdown for an order
//
//  Remittances (KRA filing)
//  GET    /v1/tax/remittances              list remittances (filter by ?taxType=VAT)
//  POST   /v1/tax/remittances              create remittance record for a period
//  PATCH  /v1/tax/remittances/{id}/status  mark as FILED or PAID
//
//  Reports
//  GET    /v1/tax/summary?from=&to=        tax summary for date range

fun Route.taxRoutes() {
    val taxService: TaxService by inject()

    route("/tax") {

        // ── Tax Rates ─────────────────────────────────────────────────────────

        route("/rates") {

            get {
                val businessId = call.businessId()
                val rates = taxService.getRates(businessId)
                call.respond(ApiResponse(true, data = rates))
            }

            post {
                val businessId = call.businessId()
                val req = call.receive<TaxRateRequest>()
                if (req.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Tax name required"))
                    return@post
                }
                if (req.rate < 0 || req.rate > 10) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Rate must be 0–1000%"))
                    return@post
                }
                val result = taxService.createRate(businessId, req)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }

            // Seed Kenya VAT / TOT / WHT / Excise defaults
            post("/seed-defaults") {
                val businessId = call.businessId()
                taxService.seedKenyaDefaults(businessId)
                call.respond(ApiResponse<Unit>(true, message = "Kenya tax defaults seeded (VAT 16%, TOT 1.5%, WHT 3%, Excise 20%)"))
            }

            route("/{id}") {

                put {
                    val businessId = call.businessId()
                    val id = call.parameters["id"]!!
                    val req = call.receive<TaxRateRequest>()
                    val result = taxService.updateRate(id, businessId, req)
                    call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
                }

                patch("/toggle") {
                    val businessId = call.businessId()
                    val id = call.parameters["id"]!!
                    val result = taxService.toggleRate(id, businessId)
                    call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
                }

                delete {
                    val businessId = call.businessId()
                    val id = call.parameters["id"]!!
                    val result = taxService.deleteRate(id, businessId)
                    call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
                }
            }
        }

        // ── Tax Calculation ───────────────────────────────────────────────────

        post("/calculate") {
            val businessId = call.businessId()
            val req = call.receive<TaxCalculationRequest>()
            if (req.amount < 0) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Amount must be positive"))
                return@post
            }
            val result = taxService.calculateTax(businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        // ── Order Tax Lines ───────────────────────────────────────────────────

        get("/orders/{orderId}") {
            val orderId = call.parameters["orderId"]!!
            val lines = taxService.getOrderTaxLines(orderId)
            call.respond(ApiResponse(true, data = lines))
        }

        // ── Remittances ───────────────────────────────────────────────────────

        route("/remittances") {

            get {
                val businessId = call.businessId()
                val taxType = call.request.queryParameters["taxType"]
                val remittances = taxService.getRemittances(businessId, taxType)
                call.respond(ApiResponse(true, data = remittances))
            }

            post {
                val businessId = call.businessId()
                val req = call.receive<TaxRemittanceRequest>()
                val result = taxService.createRemittance(businessId, req)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }

            patch("/{id}/status") {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<UpdateRemittanceStatusRequest>()
                val validStatuses = listOf("FILED", "PAID")
                if (req.status.uppercase() !in validStatuses) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Status must be FILED or PAID"))
                    return@patch
                }
                val result = taxService.updateRemittanceStatus(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }

        // ── Tax Summary Report ────────────────────────────────────────────────

        get("/summary") {
            val businessId = call.businessId()
            val from = call.request.queryParameters["from"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "from date required"))
            val to = call.request.queryParameters["to"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "to date required"))
            val result = taxService.getTaxSummary(businessId, from, to)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }
    }
}
