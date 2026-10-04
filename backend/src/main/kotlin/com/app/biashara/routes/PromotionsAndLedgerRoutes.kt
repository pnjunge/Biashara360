package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.promotionsAndLedgerPublicRoutes() {
    val customDomainService: CustomDomainService by inject()
    val promotionService: PromotionService by inject()

    route("/storefront/domain") {
        get("/resolve") {
            val host = call.request.queryParameters["host"] ?: call.request.headers["Host"].orEmpty()
            val result = customDomainService.resolveStorefrontByDomain(host)
            call.respond(ApiResponse(true, data = result))
        }
    }

    route("/storefront/coupons") {
        post("/validate") {
            val businessId = call.request.queryParameters["businessId"]
                ?: call.request.headers["X-Tenant-ID"]
            if (businessId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "businessId is required"))
                return@post
            }
            val req = call.receive<ValidateCouponRequest>()
            val res = promotionService.validateCoupon(businessId, req.code, req.orderAmount)
            call.respond(ApiResponse(true, data = res))
        }

        post("/evaluate") {
            val businessId = call.request.queryParameters["businessId"]
                ?: call.request.headers["X-Tenant-ID"]
            if (businessId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "businessId is required"))
                return@post
            }
            val req = call.receive<EvaluateCartPromotionsRequest>()
            val res = promotionService.evaluateCart(businessId, req)
            call.respond(ApiResponse(true, data = res))
        }
    }
}

fun Route.promotionsAndLedgerProtectedRoutes() {
    val promotionService: PromotionService by inject()
    val uomService: UomService by inject()
    val customDomainService: CustomDomainService by inject()
    val generalLedgerService: GeneralLedgerService by inject()

    // ─── 1. COUPONS & PROMOTIONS ───────────────────────────────────────────────
    route("/coupons") {
        get {
            val businessId = call.businessId()
            val list = promotionService.listCoupons(businessId)
            call.respond(ApiResponse(true, data = list))
        }

        post {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<CreateCouponRequest>()
            val res = promotionService.createCoupon(businessId, req)
            call.respond(if (res.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, res)
        }

        post("/{id}/toggle") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val res = promotionService.toggleCoupon(businessId, id)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }

        delete("/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@delete
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val res = promotionService.deleteCoupon(businessId, id)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }

        post("/validate") {
            val businessId = call.businessId()
            val req = call.receive<ValidateCouponRequest>()
            val res = promotionService.validateCoupon(businessId, req.code, req.orderAmount)
            call.respond(ApiResponse(true, data = res))
        }
    }

    route("/promotions") {
        get("/rules") {
            val businessId = call.businessId()
            val list = promotionService.listRules(businessId)
            call.respond(ApiResponse(true, data = list))
        }

        post("/rules") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<CreatePromotionRuleRequest>()
            val res = promotionService.createRule(businessId, req)
            call.respond(if (res.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, res)
        }

        post("/rules/{id}/toggle") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val res = promotionService.toggleRule(businessId, id)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }

        delete("/rules/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@delete
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val res = promotionService.deleteRule(businessId, id)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }

        post("/evaluate") {
            val businessId = call.businessId()
            val req = call.receive<EvaluateCartPromotionsRequest>()
            val res = promotionService.evaluateCart(businessId, req)
            call.respond(ApiResponse(true, data = res))
        }
    }

    // ─── 2. UNIT OF MEASURE (UOM) CONVERSIONS ──────────────────────────────────
    route("/products") {
        get("/{productId}/uoms") {
            val businessId = call.businessId()
            val productId = call.parameters["productId"].orEmpty()
            val list = uomService.listUoms(businessId, productId)
            call.respond(ApiResponse(true, data = list))
        }

        post("/{productId}/uoms") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val productId = call.parameters["productId"].orEmpty()
            val req = call.receive<CreateProductUomRequest>()
            val res = uomService.createUom(businessId, productId, req)
            call.respond(if (res.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, res)
        }

        delete("/uoms/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@delete
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val res = uomService.deleteUom(businessId, id)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }
    }

    // ─── 3. CUSTOM DOMAIN CONFIGURATION ────────────────────────────────────────
    route("/custom-domain") {
        get {
            val businessId = call.businessId()
            val config = customDomainService.getConfig(businessId)
            call.respond(ApiResponse(true, data = config))
        }

        post {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<SetCustomDomainRequest>()
            val res = customDomainService.setCustomDomain(businessId, req.domain)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }

        post("/verify") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val res = customDomainService.verifyCustomDomain(businessId)
            call.respond(ApiResponse(true, data = res))
        }

        delete {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@delete
            }
            val businessId = call.businessId()
            val res = customDomainService.removeCustomDomain(businessId)
            call.respond(if (res.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, res)
        }
    }

    // ─── 4. DOUBLE-ENTRY GENERAL LEDGER ────────────────────────────────────────
    route("/ledger") {
        get("/accounts") {
            val businessId = call.businessId()
            val accounts = generalLedgerService.listAccounts(businessId)
            call.respond(ApiResponse(true, data = accounts))
        }

        post("/accounts") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<CreateAccountRequest>()
            val res = generalLedgerService.createAccount(businessId, req)
            call.respond(if (res.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, res)
        }

        get("/journal-entries") {
            val businessId = call.businessId()
            val entries = generalLedgerService.listJournalEntries(businessId)
            call.respond(ApiResponse(true, data = entries))
        }

        post("/journal-entries") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<CreateJournalEntryRequest>()
            val res = generalLedgerService.createJournalEntry(businessId, call.callerUserId(), req)
            call.respond(if (res.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, res)
        }

        get("/trial-balance") {
            val businessId = call.businessId()
            val tb = generalLedgerService.getTrialBalance(businessId)
            call.respond(ApiResponse(true, data = tb))
        }
    }
}
