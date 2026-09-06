package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.ServiceManagementService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.serviceRoutes() {
    val service: ServiceManagementService by inject()
    route("/services") {
        menuGuardAny("SERVICES")

        get {
            val from = call.request.queryParameters["from"]?.let { Instant.parse(it) }
            val to = call.request.queryParameters["to"]?.let { Instant.parse(it) }
            call.respond(ApiResponse(true, data = service.schedule(call.businessId(), from, to)))
        }
        get("/catalog") { call.respond(ApiResponse(true, data = service.listServices(call.businessId()))) }
        post("/catalog") {
            call.respondService(HttpStatusCode.Created) { service.createService(call.businessId(), call.receive()) }
        }
        put("/catalog/{id}") {
            call.respondService { service.updateService(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
        }
        get("/resources") { call.respond(ApiResponse(true, data = service.listResources(call.businessId()))) }
        post("/resources") {
            call.respondService(HttpStatusCode.Created) { service.createResource(call.businessId(), call.receive()) }
        }
        put("/resources/{id}") {
            call.respondService { service.updateResource(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
        }
        get("/appointments") {
            val from = call.request.queryParameters["from"]?.let { Instant.parse(it) }
            val to = call.request.queryParameters["to"]?.let { Instant.parse(it) }
            call.respond(ApiResponse(true, data = service.listAppointments(call.businessId(), from, to)))
        }
        post("/appointments") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            call.respondService(HttpStatusCode.Created) { service.createAppointment(call.businessId(), userId, call.receive()) }
        }
        put("/appointments/{id}") {
            call.respondService { service.updateAppointment(call.businessId(), call.parameters["id"].orEmpty(), call.receive()) }
        }
        patch("/appointments/{id}/status") {
            call.respondService {
                service.updateAppointmentStatus(call.businessId(), call.parameters["id"].orEmpty(), call.receive<ServiceAppointmentStatusRequest>().status)
            }
        }
        post("/templates") { call.respond(ApiResponse(true, data = service.seedTemplates(call.businessId()))) }
    }
}

private suspend inline fun <reified T> ApplicationCall.respondService(status: HttpStatusCode = HttpStatusCode.OK, block: () -> T) {
    runCatching(block).fold(
        { respond(status, ApiResponse(true, data = it)) },
        { respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = it.message ?: "Service request failed")) },
    )
}
