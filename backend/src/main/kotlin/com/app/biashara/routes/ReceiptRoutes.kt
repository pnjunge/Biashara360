package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.models.SendEReceiptRequest
import com.app.biashara.services.ReceiptService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public routes for viewing digital e-receipts by customer.
 * Unauthenticated so anyone with the receipt link or QR code can access it.
 */
fun Route.publicReceiptRoutes() {
    val receiptService: ReceiptService by inject()

    route("/public/receipts/{orderId}") {
        get {
            val orderId = call.parameters["orderId"]?.trim()
            if (orderId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "orderId is required"))
                return@get
            }

            val receipt = receiptService.getPublicReceipt(orderId)
            if (receipt == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Receipt not found for order $orderId"))
            } else {
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = receipt))
            }
        }
    }
}

/**
 * Protected routes for merchants/cashiers to dispatch e-receipts via SMS, Email, or WhatsApp.
 */
fun Route.receiptRoutes() {
    val receiptService: ReceiptService by inject()

    route("/orders/{id}/send-ereceipt") {
        post {
            val businessId = call.businessId()
            val orderId = call.parameters["id"]?.trim()
            if (orderId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "orderId is required"))
                return@post
            }

            val req = call.receive<SendEReceiptRequest>()
            val result = receiptService.sendEReceipt(orderId, businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
        }
    }
}
