package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.services.StorefrontCatalogService
import com.app.biashara.services.StorefrontCatalogUpdate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.storefrontCatalogRoutes() {
    val service = StorefrontCatalogService()
    route("/storefront/items") {
        intercept(ApplicationCallPipeline.Call) {
            if (!call.hasRole("ADMIN", "SUPERADMIN", "BUSINESS_ADMIN", "ADMINISTRATOR")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Administrator access required to manage portal items"))
                finish()
            }
        }
        get { call.respond(ApiResponse(true, data = service.list(call.businessId()))) }
        put("/{id}") {
            val request = call.receive<StorefrontCatalogUpdate>()
            try { call.respond(ApiResponse(true, data = service.update(call.businessId(), call.parameters["id"].orEmpty(), request))) }
            catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = e.message.orEmpty())) }
        }
    }
}
