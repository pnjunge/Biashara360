package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.PurchaseInvoiceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.purchaseRoutes() {
    val service: PurchaseInvoiceService by inject()

    route("/purchases") {
        moduleGuard("INVENTORY")
        menuGuardAny("PURCHASES")
        permissionGuard("purchases.view")
        get {
            val businessId = call.businessId()
            val query = call.request.queryParameters["q"]
            val result = service.list(businessId, query)
            call.respond(HttpStatusCode.OK, result)
        }

        get("/{id}") {
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val result = service.getById(id, businessId)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }

        post {
            if (!call.hasPermission("purchases.create")) return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message="Create purchase permission required"))
            val businessId = call.businessId()
            val req = call.receive<CreatePurchaseInvoiceRequest>()
            val result = service.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }
    }
}
