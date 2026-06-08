package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.BusinessSettingsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// ─── Business Settings Routes ─────────────────────────────────────────────────
// Admin only.
//
//   GET /v1/settings/mpesa          get current Mpesa config (secrets masked)
//   PUT /v1/settings/mpesa          save / update Mpesa config
//
//   GET /v1/settings/cybersource    get current CyberSource config (secret masked)
//   PUT /v1/settings/cybersource    save / update CyberSource config

fun Route.businessSettingsRoutes() {
    val settingsService: BusinessSettingsService by inject()

    route("/settings") {

        // ── Mpesa ─────────────────────────────────────────────────────────────

        route("/mpesa") {
            get {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@get
                }
                val businessId = call.businessId()
                val config = settingsService.getMpesaConfig(businessId)
                if (config == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Mpesa not configured yet"))
                } else {
                    call.respond(ApiResponse(true, data = config))
                }
            }

            put {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@put
                }
                val businessId = call.businessId()
                val req = call.receive<MpesaConfigRequest>()
                val result = settingsService.saveMpesaConfig(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }

        // ── CyberSource ───────────────────────────────────────────────────────

        route("/cybersource") {
            get {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@get
                }
                val businessId = call.businessId()
                val config = settingsService.getCyberSourceConfig(businessId)
                if (config == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "CyberSource not configured yet"))
                } else {
                    call.respond(ApiResponse(true, data = config))
                }
            }

            put {
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@put
                }
                val businessId = call.businessId()
                val req = call.receive<CyberSourceConfigRequest>()
                val result = settingsService.saveCyberSourceConfig(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }
    }
}
