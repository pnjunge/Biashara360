package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SuperAdminService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// ─── Super Admin Routes ───────────────────────────────────────────────────────
// All routes require SUPERADMIN role.
//
//   GET  /v1/admin/businesses        list all businesses
//   POST /v1/admin/businesses        create a business with its first admin user

fun Route.superAdminRoutes() {
    val superAdminService: SuperAdminService by inject()

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
    }
}
