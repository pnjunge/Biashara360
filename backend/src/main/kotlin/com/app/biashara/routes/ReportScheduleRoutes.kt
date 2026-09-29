package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.ReportSchedulerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class ToggleActiveRequest(val isActive: Boolean)

fun Route.reportScheduleRoutes() {
    val schedulerService: ReportSchedulerService by inject()

    route("/report-schedules") {
        moduleGuard("REPORTS")

        // List schedules for business
        get {
            val businessId = call.businessId()
            val schedules = schedulerService.listSchedules(businessId)
            call.respond(ApiResponse(true, data = schedules))
        }

        // Create new schedule
        post {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "MANAGER")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Manager or Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val req = call.receive<CreateReportScheduleRequest>()
            val result = schedulerService.createSchedule(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        // Get single schedule
        get("/{id}") {
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val schedule = schedulerService.getSchedule(id, businessId)
            if (schedule == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Report schedule not found"))
            } else {
                call.respond(ApiResponse(true, data = schedule))
            }
        }

        // Update schedule
        put("/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "MANAGER")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Manager or Admin access required"))
                return@put
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val req = call.receive<UpdateReportScheduleRequest>()
            val result = schedulerService.updateSchedule(id, businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        // Delete schedule
        delete("/{id}") {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "MANAGER")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Manager or Admin access required"))
                return@delete
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val result = schedulerService.deleteSchedule(id, businessId)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }

        // Toggle active status
        post("/{id}/toggle-active") {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "MANAGER")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Manager or Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val req = call.receive<ToggleActiveRequest>()
            val result = schedulerService.toggleActive(id, businessId, req.isActive)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }

        // Immediate "Send Now" test dispatch
        post("/{id}/send-now") {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "MANAGER")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Manager or Admin access required"))
                return@post
            }
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val result = schedulerService.executeSchedule(id, businessId, isManualTest = true)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, ApiResponse(result.success, data = result, message = result.message))
        }

        // Schedule execution logs
        get("/{id}/logs") {
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val logs = schedulerService.getLogs(businessId, id)
            call.respond(ApiResponse(true, data = logs))
        }

        // All execution logs for business
        get("/logs/all") {
            val businessId = call.businessId()
            val logs = schedulerService.getLogs(businessId, null)
            call.respond(ApiResponse(true, data = logs))
        }
    }
}
