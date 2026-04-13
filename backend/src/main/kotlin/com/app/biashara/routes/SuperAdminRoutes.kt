package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SuperAdminService
import com.app.biashara.services.SystemSettingsService
import io.ktor.http.*
import io.ktor.server.application.*
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
                val req = call.receive<CreateBusinessWithAdminRequest>()
                val result = superAdminService.createBusinessWithAdmin(req)
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
