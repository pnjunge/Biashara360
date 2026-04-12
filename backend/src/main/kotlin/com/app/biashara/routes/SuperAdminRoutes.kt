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
//
// All endpoints require SUPERADMIN role.
//
//  GET  /v1/admin/businesses          list all businesses
//  POST /v1/admin/businesses          create a new business + admin user
//  GET  /v1/admin/businesses/{id}     get a single business

fun Route.superAdminRoutes() {
    val superAdminService: SuperAdminService by inject()

    route("/admin/businesses") {

        get {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "SUPERADMIN access required"))
                return@get
            }
            val result = superAdminService.listBusinesses()
            call.respond(HttpStatusCode.OK, result)
        }

        post {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "SUPERADMIN access required"))
                return@post
            }
            val req = call.receive<CreateBusinessRequest>()
            val result = superAdminService.createBusinessWithAdmin(req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        get("/{id}") {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "SUPERADMIN access required"))
                return@get
            }
            val id       = call.parameters["id"]!!
            val business = superAdminService.getBusiness(id)
            if (business == null)
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Business not found"))
            else
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = business))
        }
    }
}
