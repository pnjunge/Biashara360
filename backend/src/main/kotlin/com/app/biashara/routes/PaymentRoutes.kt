package com.app.biashara.routes

import com.app.biashara.auth.generateId
import com.app.biashara.constants.Constants
import com.app.biashara.db.*
import com.app.biashara.exceptions.NotFoundException
import com.app.biashara.exceptions.UnauthorizedException
import com.app.biashara.models.*
import com.app.biashara.services.MpesaService
import com.app.biashara.utils.SignatureUtils
import com.app.biashara.validation.Validator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.ResultRow
import org.koin.ktor.ext.inject

/**
 * Payment routes with validation and signature verification.
 */
fun Route.paymentRoutesValidated() {
    val mpesaService: MpesaService by inject()

    route("/payments") {
        moduleGuard("PAYMENTS")
        
        /**
         * Initiate M-Pesa STK Push payment
         * POST /payments/mpesa/initiate
         */
        post("/mpesa/initiate") {
            val businessId = call.businessId()
            val req = call.receive<InitiatePaymentRequest>()
            
            // Validate payment initiation request
            Validator.validate {
                field("orderId", req.orderId) {
                    required()
                    uuid()
                }
                field("phoneNumber", req.phoneNumber) {
                    required()
                    phone()
                }
            }
            
            // 🔒 SECURITY: Idempotency check - prevent duplicate payment requests
            val existingAttempt = transaction {
                val order = OrdersTable.select {
                    (OrdersTable.id eq req.orderId) and (OrdersTable.businessId eq businessId)
                }.firstOrNull() ?: return@transaction null
                
                // Check if order already has a pending/successful payment
                if (order[OrdersTable.paymentStatus] in setOf("PAID", "PROCESSING")) {
                    return@transaction "duplicate"
                }
                
                // Check if there's a recent checkout attempt (within last 2 minutes)
                val checkoutRequestId = order[OrdersTable.stkCheckoutRequestId]
                if (!checkoutRequestId.isNullOrBlank()) {
                    val recentAttempt = MpesaCheckoutAttemptsTable.select {
                        (MpesaCheckoutAttemptsTable.checkoutRequestId eq checkoutRequestId) and
                        (MpesaCheckoutAttemptsTable.orderId eq req.orderId)
                    }.orderBy(MpesaCheckoutAttemptsTable.createdAt, SortOrder.DESC)
                    .firstOrNull()
                    
                    if (recentAttempt != null) {
                        val attemptTime = recentAttempt[MpesaCheckoutAttemptsTable.createdAt]
                        val now = Clock.System.now()
                        val ageSeconds = (now - attemptTime).inWholeSeconds
                        
                        if (ageSeconds < 120) { // 2 minutes
                            return@transaction "recent"
                        }
                    }
                }
                
                order
            }
            
            when (existingAttempt) {
                "duplicate" -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(false, message = "Payment already processed or in progress for this order")
                    )
                    return@post
                }
                "recent" -> {
                    call.respond(
                        HttpStatusCode.TooEarly,
                        ApiResponse<Unit>(false, message = "A payment request was recently sent. Please wait before retrying.")
                    )
                    return@post
                }
                null -> throw NotFoundException("Order", req.orderId)
            }
            
            val order = existingAttempt as ResultRow
            val amount = order[OrdersTable.subtotal]
            
            // Validate amount
            Validator.validate {
                field("amount", amount) {
                    validAmount(
                        min = Constants.Mpesa.MIN_AMOUNT,
                        max = Constants.Mpesa.MAX_AMOUNT
                    )
                }
            }
            
            // Initiate STK push
            val result = mpesaService.initiateSTKPush(
                phoneNumber = req.phoneNumber,
                amount = amount,
                accountReference = order[OrdersTable.orderNumber],
                transactionDesc = "Payment for order ${order[OrdersTable.orderNumber]}",
                businessId = businessId,
                accountType = req.accountType
            )
            
            when (result) {
                is com.app.biashara.services.StkPushResult.Success -> {
                    // Store checkout request ID for callback matching
                    transaction {
                        OrdersTable.update({ OrdersTable.id eq req.orderId }) {
                            it[stkCheckoutRequestId] = result.checkoutRequestId
                            it[updatedAt] = Clock.System.now()
                        }
                    }
                    
                    call.respond(
                        HttpStatusCode.OK,
                        StkPushResponse(
                            merchantRequestId = result.merchantRequestId,
                            checkoutRequestId = result.checkoutRequestId,
                            responseCode = result.responseCode,
                            responseDescription = result.customerMessage,
                            customerMessage = result.customerMessage
                        )
                    )
                }
                is com.app.biashara.services.StkPushResult.Error -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(false, message = result.message)
                    )
                }
            }
        }

        /**
         * Reconcile payment manually
         * POST /payments/reconcile
         */
        post("/reconcile") {
            val businessId = call.businessId()
            val req = call.receive<ReconcileRequest>()
            
            // Validate reconciliation request
            Validator.validate {
                field("orderId", req.orderId) {
                    required()
                    uuid()
                }
            }
            
            // Check if order exists
            val order = transaction {
                OrdersTable.select {
                    (OrdersTable.id eq req.orderId) and (OrdersTable.businessId eq businessId)
                }.firstOrNull()
            } ?: throw NotFoundException("Order", req.orderId)
            
            call.respond(
                ApiResponse(
                    success = true,
                    message = "Reconciliation check initiated",
                    data = mapOf(
                        "orderId" to req.orderId,
                        "currentStatus" to order[OrdersTable.paymentStatus]
                    )
                )
            )
        }

        post("/mpesa/transaction-query") {
            val businessId = call.businessId()
            val req = call.receive<MpesaTransactionQueryRequest>()
            Validator.validate { field("transactionId", req.transactionId) { required() } }
            val result = mpesaService.queryTransaction(req.transactionId, businessId)
            call.respond(
                if (result.success) HttpStatusCode.OK else HttpStatusCode.BadGateway,
                ApiResponse(result.success, message = result.message, data = result.response)
            )
        }
    }
}

/**
 * M-Pesa callback route with signature verification
 * This route is called by Safaricom and does NOT require JWT auth
 */
fun Route.mpesaCallbackRouteValidated() {
    post("/payments/mpesa/callback") {
        val rawBody = call.receiveText()
        
        // Parse callback
        val callback = try {
            val lenientJson = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            lenientJson.decodeFromString<MpesaCallbackRequest>(rawBody)
        } catch (e: Exception) {
            application.log.warn("""{"event":"mpesa_callback_rejected","reason":"invalid_payload"}""")
            call.respond(HttpStatusCode.OK, DarajaAck())
            return@post
        }
        
        val stkCallback = callback.Body.stkCallback
        val checkoutRequestId = stkCallback.CheckoutRequestID
        
        // Validate callback authenticity
        val timestamp = stkCallback.CallbackMetadata?.Item
            ?.find { it.Name == "TransactionDate" }?.Value
        
        if (!SignatureUtils.validateMpesaCallback(
                checkoutRequestId = checkoutRequestId,
                timestamp = timestamp,
                maxAgeSeconds = Constants.Mpesa.CALLBACK_TIMEOUT_SECONDS.toLong()
            )) {
            application.log.warn("""{"event":"mpesa_callback_rejected","reason":"invalid_signature"}""")
            call.respond(HttpStatusCode.Unauthorized, DarajaAck(ResultCode = 1, ResultDesc = "Invalid callback"))
            return@post
        }
        
        // Process successful payment
        if (stkCallback.ResultCode == 0) {
            val metadata = stkCallback.CallbackMetadata?.Item ?: emptyList()
            val amount = metadata.find { it.Name == "Amount" }?.Value?.toDoubleOrNull() ?: 0.0
            val txCode = metadata.find { it.Name == "MpesaReceiptNumber" }?.Value ?: ""
            val phone = metadata.find { it.Name == "PhoneNumber" }?.Value ?: ""
            val payerName = metadata.find { it.Name == "FirstName" }?.Value ?: "Unknown"
            
            transaction {
                val orderRow = OrdersTable
                    .select { OrdersTable.stkCheckoutRequestId eq checkoutRequestId }
                    .firstOrNull()
                
                if (orderRow != null) {
                    val businessId = orderRow[OrdersTable.businessId]
                    val orderId = orderRow[OrdersTable.id]
                    val orderSubtotal = orderRow[OrdersTable.subtotal]
                    val now = Clock.System.now()
                    
                    // 🔒 SECURITY: Idempotency check - prevent duplicate payment processing
                    // Check if payment with this transaction code already exists
                    val existingPayment = PaymentsTable.select {
                        (PaymentsTable.transactionCode eq txCode) and
                        (PaymentsTable.businessId eq businessId)
                    }.firstOrNull()
                    
                    if (existingPayment != null) {
                        application.log.warn("""{"event":"mpesa_callback_duplicate","transaction_code":"${txCode.take(8)}***","order_id":"$orderId"}""")
                        // Already processed, acknowledge without re-processing
                        return@transaction
                    }
                    
                    // Check if order is already marked as paid
                    if (orderRow[OrdersTable.paymentStatus] == "PAID") {
                        application.log.warn("""{"event":"mpesa_callback_already_paid","order_id":"$orderId"}""")
                        return@transaction
                    }
                    
                    // Validate amount matches order (allow 1% tolerance for rounding)
                    if (Math.abs(amount - orderSubtotal) > orderSubtotal * 0.01) {
                        application.log.warn("""{"event":"mpesa_callback_rejected","reason":"amount_mismatch","order_id":"$orderId","expected":$orderSubtotal,"received":$amount}""")
                        // Don't process payment if amount doesn't match
                        return@transaction
                    }
                    
                    // Save payment record
                    PaymentsTable.insert {
                        it[PaymentsTable.id] = generateId()
                        it[PaymentsTable.businessId] = businessId
                        it[PaymentsTable.orderId] = orderId
                        it[PaymentsTable.billingOwnerUserId] = orderRow[OrdersTable.billingOwnerUserId]
                        it[PaymentsTable.transactionCode] = txCode
                        it[PaymentsTable.amount] = amount
                        it[PaymentsTable.payerPhone] = phone
                        it[PaymentsTable.payerName] = payerName
                        it[PaymentsTable.method] = "MPESA"
                        it[PaymentsTable.status] = "SUCCESS"
                        it[PaymentsTable.channel] = "STK_PUSH"
                        it[PaymentsTable.reconciled] = true
                        it[PaymentsTable.transactionDate] = now
                    }
                    
                    // Mark order as paid
                    OrdersTable.update({ OrdersTable.id eq orderId }) {
                        it[OrdersTable.paymentStatus] = "PAID"
                        it[OrdersTable.mpesaTransactionCode] = txCode
                        it[OrdersTable.updatedAt] = now
                    }
                    ServiceAppointmentsTable.update({ ServiceAppointmentsTable.orderId eq orderId }) {
                        it[status] = "COMPLETED"
                        it[updatedAt] = now
                    }
                    
                    application.log.info("""{"event":"payment_completed","provider":"mpesa","order_id":"$orderId"}""")
                } else {
                    application.log.warn("""{"event":"mpesa_callback_unmatched","checkout_request_id":"${checkoutRequestId.take(8)}***"}""")
                }
            }
        } else {
            application.log.warn("""{"event":"payment_failure","provider":"mpesa","result_code":${stkCallback.ResultCode}}""")
        }
        
        // Always acknowledge to Safaricom
        call.respond(HttpStatusCode.OK, DarajaAck())
    }

    // Daraja transaction-status callbacks are asynchronous. Keep the
    // endpoints public (Safaricom cannot present our JWT) and acknowledge
    // promptly; detailed reconciliation remains an explicit business action.
    post("/payments/mpesa/transaction-query/result") {
        call.receiveText()
        call.respond(HttpStatusCode.OK, DarajaAck())
    }
    post("/payments/mpesa/transaction-query/timeout") {
        call.receiveText()
        call.respond(HttpStatusCode.OK, DarajaAck(ResultCode = 1, ResultDesc = "Timed out"))
    }
}
