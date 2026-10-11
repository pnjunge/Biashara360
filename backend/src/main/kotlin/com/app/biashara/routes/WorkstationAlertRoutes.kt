package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// A merchant-bound, read-only device capability; it is never a staff session.
fun Route.workstationAlertPublicRoutes() {
    get("/workstation-alerts/feed") {
        val data = WorkstationAlertService().feed(call.request.headers["X-Workstation-Key"].orEmpty())
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        if (data == null) call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(false, message="Workstation link expired or revoked. Ask your merchant administrator to relink."))
        else call.respond(ApiResponse(true, data=data))
    }
}
fun Route.workstationAlertAdminRoutes() {
    route("/workstation-alerts") {
        intercept(ApplicationCallPipeline.Call) {
            if (!call.hasRole("ADMIN", "SUPERADMIN") || !WorkstationAlertService().isAdministrator(call.businessId(), call.callerUserId())) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false,message="A merchant administrator must manage workstation links")); finish()
            }
        }
        get { call.respond(ApiResponse(true,data=WorkstationAlertService().list(call.businessId()))) }
        post {
            try { call.respond(HttpStatusCode.Created, ApiResponse(true,data=WorkstationAlertService().link(call.businessId(),call.receive<WorkstationLinkRequest>().name))) }
            catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message=e.message.orEmpty())) }
        }
        delete("/{id}") {
            val revoked = WorkstationAlertService().revoke(call.businessId(),call.parameters["id"].orEmpty())
            call.respond(if(revoked) HttpStatusCode.OK else HttpStatusCode.NotFound,ApiResponse<Unit>(revoked,message=if(revoked) "Workstation link revoked" else "Workstation not found"))
        }
    }
}
