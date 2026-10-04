package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.SupplierService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.supplierRoutes() {
    val service: SupplierService by inject()

    route("/suppliers") {
        moduleGuard("INVENTORY")
        menuGuardAny("INVENTORY", "PURCHASES")
        permissionGuard("inventory.suppliers")
        get {
            val businessId = call.businessId()
            val list = service.list(businessId)
            call.respond(HttpStatusCode.OK, ApiResponse(true, data = list))
        }

        post {
            val businessId = call.businessId()
            val req = call.receive<SupplierRequest>()
            val result = service.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        put("/{id}") {
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val req = call.receive<SupplierRequest>()
            val result = service.update(businessId, id, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }

        delete("/{id}") {
            val businessId = call.businessId()
            val id = call.parameters["id"].orEmpty()
            val result = service.delete(businessId, id)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }
    }
}
