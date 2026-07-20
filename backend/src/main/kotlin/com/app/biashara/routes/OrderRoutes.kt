package com.app.biashara.routes

import com.app.biashara.constants.Constants
import com.app.biashara.constants.PaymentStatus
import com.app.biashara.constants.PaymentMethod
import com.app.biashara.constants.DeliveryStatus
import com.app.biashara.exceptions.NotFoundException
import com.app.biashara.models.*
import com.app.biashara.services.OrderService
import com.app.biashara.utils.PaginationParams
import com.app.biashara.validation.Validator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Order management routes with comprehensive validation.
 */
fun Route.orderRoutesValidated() {
    val orderService: OrderService by inject()

    route("/orders") {
        moduleGuard("SALES")
        
        /**
         * List all orders with pagination and filters
         * GET /orders?paymentStatus=PENDING&page=1&pageSize=20
         */
        get {
            val businessId = call.businessId()
            val paymentStatus = call.request.queryParameters["paymentStatus"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() 
                ?: Constants.Business.DEFAULT_PAGE_SIZE
            
            // Validate payment status if provided
            if (paymentStatus != null && !PaymentStatus.isValid(paymentStatus)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Invalid payment status: $paymentStatus")
                )
                return@get
            }
            
            // Validate pagination
            Validator.validate {
                field("page", page) {
                    positive()
                }
                field("pageSize", pageSize) {
                    positive()
                    max(Constants.Business.MAX_PAGE_SIZE.toDouble())
                }
            }
            
            val result = orderService.getAll(businessId, paymentStatus, page, pageSize)
            call.respond(result)
        }

        /**
         * Create new order
         * POST /orders
         */
        post {
            val businessId = call.businessId()
            val req = call.receive<CreateOrderRequest>()
            
            // Validate order creation request
            Validator.validate {
                field("customerName", req.customerName) {
                    required()
                    length(2, 255)
                }
                field("customerPhone", req.customerPhone) {
                    required()
                    phone()
                }
                field("deliveryLocation", req.deliveryLocation) {
                    maxLength(500)
                }
                field("items", req.items) {
                    required()
                    notEmpty()
                    maxSize(Constants.Business.MAX_ORDER_ITEMS)
                }
                field("paymentMethod", req.paymentMethod) {
                    required()
                    custom("Invalid payment method", "INVALID_PAYMENT_METHOD") {
                        PaymentMethod.isValid(it as String)
                    }
                }
                field("notes", req.notes) {
                    maxLength(1000)
                }
                field("customerId", req.customerId) {
                    optional {
                        uuid()
                    }
                }
            }
            
            // Validate each order item
            req.items.forEachIndexed { index, item ->
                Validator.validate {
                    field("items[$index].productId", item.productId) {
                        required()
                        uuid()
                    }
                    field("items[$index].quantity", item.quantity) {
                        required()
                        positive()
                        max(10000.0)
                    }
                    field("items[$index].unitPrice", item.unitPrice) {
                        required()
                        validAmount(
                            min = Constants.Business.MIN_ORDER_AMOUNT,
                            max = Constants.Business.MAX_ORDER_AMOUNT
                        )
                    }
                }
            }
            
            val result = orderService.create(businessId, req)
            call.respond(
                if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                result
            )
        }

        route("/{id}") {
            /**
             * Get order by ID
             * GET /orders/{id}
             */
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Order ID required")
                
                // Validate UUID
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                }
                
                val order = orderService.getById(id, businessId)
                    ?: throw NotFoundException("Order", id)
                
                call.respond(ApiResponse(true, data = order))
            }

            /**
             * Update payment status
             * PATCH /orders/{id}/payment-status
             */
            patch("/payment-status") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Order ID required")
                val req = call.receive<UpdatePaymentStatusRequest>()
                
                // Validate request
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                    field("status", req.status) {
                        required()
                        custom("Invalid payment status", "INVALID_STATUS") {
                            PaymentStatus.isValid(it as String)
                        }
                    }
                    field("mpesaTransactionCode", req.mpesaTransactionCode) {
                        optional {
                            minLength(10)
                            maxLength(50)
                            alphanumeric()
                        }
                    }
                }
                
                val result = orderService.updatePaymentStatus(id, businessId, req)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    result
                )
            }

            /**
             * Update delivery status
             * PATCH /orders/{id}/delivery-status
             */
            patch("/delivery-status") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Order ID required")
                val req = call.receive<UpdateDeliveryStatusRequest>()
                
                // Validate request
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                    field("status", req.status) {
                        required()
                        custom("Invalid delivery status", "INVALID_STATUS") {
                            DeliveryStatus.isValid(it as String)
                        }
                    }
                }
                
                val result = orderService.updateDeliveryStatus(id, businessId, req)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    result
                )
            }

            /**
             * Cancel order
             * POST /orders/{id}/cancel
             */
            post("/cancel") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Order ID required")
                
                // Validate UUID
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                }
                
                val result = orderService.cancel(id, businessId)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    result
                )
            }
        }
    }
}
