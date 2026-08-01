package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.StorefrontService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.storefrontRoutes() {
    val storefrontService: StorefrontService by inject()

    route("/public/store/{storeIdentifier}") {
        get {
            val storeIdentifier = call.parameters["storeIdentifier"].orEmpty()
            val storefront = storefrontService.getStorefront(storeIdentifier)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(false, message = "Store not found")
                )
            call.respond(ApiResponse(true, data = storefront))
        }

        rateLimit(RateLimitName("public-payment-limiter")) {
            post("/checkout") {
                val storeIdentifier = call.parameters["storeIdentifier"].orEmpty()
                val request = call.receive<StorefrontCheckoutRequest>()
                val result = storefrontService.checkout(storeIdentifier, request)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }

            post("/orders/{orderId}/mpesa") {
                val storeIdentifier = call.parameters["storeIdentifier"].orEmpty()
                val orderId = call.parameters["orderId"].orEmpty()
                val request = call.receive<StorefrontPaymentRetryRequest>()
                val result = storefrontService.retryPayment(
                    storeIdentifier,
                    orderId,
                    request.clientReference,
                    request.customerPhone
                )
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }

        get("/orders/{orderId}") {
            val storeIdentifier = call.parameters["storeIdentifier"].orEmpty()
            val orderId = call.parameters["orderId"].orEmpty()
            val reference = call.request.queryParameters["reference"].orEmpty()
            val status = storefrontService.getOrderStatus(storeIdentifier, orderId, reference)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(false, message = "Order not found")
                )
            call.respond(ApiResponse(true, data = status))
        }
    }
}
