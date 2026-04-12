package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SettingsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// ─── Settings Routes ──────────────────────────────────────────────────────────
//
// Business admins use these endpoints to configure integration parameters.
// All endpoints require ADMIN or SUPERADMIN role.
//
//  GET  /v1/settings/cybersource   get current CyberSource configuration
//  POST /v1/settings/cybersource   save/update CyberSource configuration
//
//  GET  /v1/settings/mpesa         get current M-Pesa (Daraja) configuration
//  POST /v1/settings/mpesa         save/update M-Pesa (Daraja) configuration

fun Route.settingsRoutes() {
    val settingsService: SettingsService by inject()

    route("/settings") {

        // ── CyberSource ────────────────────────────────────────────────────────

        route("/cybersource") {

            get {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@get
                }
                val result = settingsService.getCyberSourceSettings(businessId)
                call.respond(HttpStatusCode.OK, result)
            }

            post {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@post
                }
                val req    = call.receive<CyberSourceSettingsRequest>()
                val result = settingsService.saveCyberSourceSettings(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }

        // ── M-Pesa ─────────────────────────────────────────────────────────────

        route("/mpesa") {

            get {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@get
                }
                val result = settingsService.getMpesaSettings(businessId)
                call.respond(HttpStatusCode.OK, result)
            }

            post {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@post
                }
                val req    = call.receive<MpesaSettingsRequest>()
                val result = settingsService.saveMpesaSettings(businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }
    }
}
