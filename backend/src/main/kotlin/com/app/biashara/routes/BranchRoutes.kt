package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.models.BranchRequest
import com.app.biashara.services.BranchService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.branchRoutes() {
    val branchService: BranchService by inject()

    route("/branches") {
        get {
            val businessId = call.businessId()
            val includeInactive = call.request.queryParameters["includeInactive"]?.toBoolean() ?: false
            val branches = branchService.listBranches(businessId, includeInactive)
            call.respond(ApiResponse(true, data = branches))
        }

        post {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required to create branches"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<BranchRequest>()
            val ipAddress = call.request.origin.remoteHost
            val result = branchService.createBranch(businessId, req, call.callerUserId(), ipAddress)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        get("/{id}") {
            val businessId = call.businessId()
            val branchId = call.parameters["id"].orEmpty()
            val branch = branchService.getBranch(businessId, branchId)
            if (branch == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Branch not found"))
            } else {
                call.respond(ApiResponse(true, data = branch))
            }
        }

        put("/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required to update branches"))
                return@put
            }
            val businessId = call.businessId()
            val branchId = call.parameters["id"].orEmpty()
            val req = call.receive<BranchRequest>()
            val ipAddress = call.request.origin.remoteHost
            val result = branchService.updateBranch(businessId, branchId, req, call.callerUserId(), ipAddress)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        delete("/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required to delete branches"))
                return@delete
            }
            val businessId = call.businessId()
            val branchId = call.parameters["id"].orEmpty()
            val ipAddress = call.request.origin.remoteHost
            val result = branchService.deleteBranch(businessId, branchId, call.callerUserId(), ipAddress)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        post("/{id}/set-head-office") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required to manage branches"))
                return@post
            }
            val businessId = call.businessId()
            val branchId = call.parameters["id"].orEmpty()
            val ipAddress = call.request.origin.remoteHost
            val result = branchService.setHeadOffice(businessId, branchId, call.callerUserId(), ipAddress)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }
    }
}
