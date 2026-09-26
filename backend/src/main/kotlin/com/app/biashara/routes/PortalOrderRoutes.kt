package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.services.PortalOrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Registered inside JWT authentication. Every active user of the business may claim portal orders.
fun Route.portalOrderRoutes() {
    val service = PortalOrderService()
    route("/portal-orders") {
        get {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            try {
                call.respond(ApiResponse(true, data = service.queue(call.businessId(), userId)))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = e.message.orEmpty()))
            }
        }
        post("/{id}/claim") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            try {
                val order = service.claim(call.businessId(), userId, call.parameters["id"].orEmpty())
                if (order == null) call.respond(HttpStatusCode.Conflict, ApiResponse<Unit>(false,
                    message = "This order was already claimed or is no longer available."))
                else call.respond(ApiResponse(true, data = order, message = "Order claimed"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = e.message.orEmpty()))
            }
        }
    }
}
