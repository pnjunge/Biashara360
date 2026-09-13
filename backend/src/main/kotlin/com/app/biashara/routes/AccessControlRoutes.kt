package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.AccessControlService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.accessControlRoutes() {
    val service: AccessControlService by inject()

    route("/access") {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            call.respond(ApiResponse(true, data = service.myMenus(call.businessId(), principal.payload.subject, call.userRole())))
        }

        route("/config") {
            intercept(ApplicationCallPipeline.Call) {
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    finish()
                }
            }
            get { call.respond(ApiResponse(true, data = service.config(call.businessId()))) }
            put("/menus") {
                call.respondResult { service.updateMenus(call.businessId(), call.receive()) }
            }
            post("/roles") {
                call.respondResult(HttpStatusCode.Created) { service.createRole(call.businessId(), call.receive()) }
            }
            put("/roles/{id}") {
                call.respondResult { service.updateRole(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
            }
            delete("/roles/{id}") {
                call.respondResult { service.deleteRole(call.businessId(), call.parameters["id"].orEmpty()) }
            }
            patch("/roles/{id}/status") {
                val req = call.receive<UpdateAccessStatusRequest>()
                call.respondResult { service.toggleRoleStatus(call.businessId(), call.parameters["id"].orEmpty(), req.isActive) }
            }
            post("/groups") {
                call.respondResult(HttpStatusCode.Created) { service.createGroup(call.businessId(), call.receive()) }
            }
            put("/groups/{id}") {
                call.respondResult { service.updateGroup(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
            }
            delete("/groups/{id}") {
                call.respondResult { service.deleteGroup(call.businessId(), call.parameters["id"].orEmpty()) }
            }
            patch("/groups/{id}/status") {
                val req = call.receive<UpdateAccessStatusRequest>()
                call.respondResult { service.toggleGroupStatus(call.businessId(), call.parameters["id"].orEmpty(), req.isActive) }
            }
            put("/groups/{id}/users") {
                call.respondResult { service.assignUsers(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
            }
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.respondResult(
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    block: () -> T
) {
    runCatching(block).fold(
        onSuccess = { respond(successStatus, ApiResponse(true, data = it)) },
        onFailure = { respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = it.message ?: "Invalid access configuration")) }
    )
}
