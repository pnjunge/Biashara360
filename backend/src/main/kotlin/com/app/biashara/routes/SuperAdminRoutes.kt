package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SuperAdminService
import com.app.biashara.services.SystemSettingsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// ─── Super Admin Routes ───────────────────────────────────────────────────────
// All routes require SUPERADMIN role.
//
//   GET  /v1/admin/businesses                   list all businesses
//   POST /v1/admin/businesses                   create a business with its first admin user
//   GET  /v1/admin/settings/mpesa-callback      get system-wide Mpesa callback URL
//   PUT  /v1/admin/settings/mpesa-callback      update system-wide Mpesa callback URL

fun Route.superAdminRoutes() {
    val superAdminService: SuperAdminService by inject()
    val systemSettingsService: SystemSettingsService by inject()

    route("/admin") {

        route("/businesses") {
            get {
                if (!call.hasRole("SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                    return@get
                }
                val businesses = superAdminService.listBusinesses()
                call.respond(ApiResponse(true, data = businesses))
            }

            post {
                if (!call.hasRole("SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                    return@post
                }
                val req = try {
                    call.receive<CreateBusinessWithAdminRequest>()
                } catch (e: BadRequestException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Invalid request body: ${e.message ?: "malformed JSON or missing required fields"}"))
                    return@post
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Could not parse request: ${e.message ?: "unknown error"}"))
                    return@post
                }
                val result = try {
                    superAdminService.createBusinessWithAdmin(req)
                } catch (e: Exception) {
                    call.application.log.error("Error creating business with admin", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, message = "An internal error occurred while creating the business"))
                    return@post
                }
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }
        }

        route("/settings") {

            // ── System-wide Mpesa callback URL ────────────────────────────────

            route("/mpesa-callback") {
                get {
                    if (!call.hasRole("SUPERADMIN")) {
                        call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                        return@get
                    }
                    val url = systemSettingsService.getMpesaCallbackUrl()
                    if (url == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Mpesa callback URL not configured"))
                    } else {
                        call.respond(ApiResponse(true, data = SystemSettingResponse("mpesa_callback_url", url)))
                    }
                }

                put {
                    if (!call.hasRole("SUPERADMIN")) {
                        call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                        return@put
                    }
                    val req = call.receive<SystemSettingRequest>()
                    val result = systemSettingsService.saveMpesaCallbackUrl(req.value)
                    call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
                }
            }
        }
    }
}
