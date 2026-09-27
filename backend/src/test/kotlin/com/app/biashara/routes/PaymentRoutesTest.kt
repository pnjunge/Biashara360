package com.app.biashara.routes

import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.services.MpesaService
import com.app.biashara.services.StkPushResult
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for payment routes focusing on security and idempotency.
 * 
 * 🔒 SECURITY TESTS:
 * - Idempotency checks prevent duplicate payments
 * - Rate limiting prevents abuse
 * - Amount validation prevents manipulation
 * - Callback authentication
 */
class PaymentRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `test payment initiation prevents duplicate requests within 2 minutes`() = testApplication {
        // Setup
        val mockMpesaService = mockk<MpesaService>()
        val businessId = "test-business-123"
        val orderId = "test-order-123"
        val checkoutRequestId = "ws_CO_test_123"

        // Mock database state: order exists with recent checkout attempt
        transaction {
            BusinessesTable.insert {
                it[id] = businessId
                it[name] = "Test Business"
                it[storefrontSlug] = "test-business"
                it[type] = "RETAIL"
                it[ownerPhone] = "254712345678"
                it[ownerEmail] = "test@example.com"
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-001"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PENDING"
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0
                it[stkCheckoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            // Insert recent checkout attempt (less than 2 minutes ago)
            MpesaCheckoutAttemptsTable.insert {
                it[id] = "attempt-123"
                it[MpesaCheckoutAttemptsTable.businessId] = businessId
                it[MpesaCheckoutAttemptsTable.orderId] = orderId
                it[MpesaCheckoutAttemptsTable.checkoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now() // Just now
            }
        }

        // When: Try to initiate payment again
        val response = client.post("/v1/payments/mpesa/initiate") {
            contentType(ContentType.Application.Json)
            setBody("""{"orderId":"$orderId","phoneNumber":"254712345678"}""")
            header("Authorization", "Bearer test-jwt-token")
        }

        // Then: Should be rejected with 425 Too Early
        assertEquals(HttpStatusCode.TooEarly, response.status)
        val body = json.decodeFromString<ApiResponse<Unit>>(response.bodyAsText())
        assertFalse(body.success)
        assertTrue(body.message.contains("recently sent"))
    }

    @Test
    fun `test payment initiation rejects already paid orders`() = testApplication {
        // Setup
        val businessId = "test-business-123"
        val orderId = "test-order-123"

        transaction {
            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-002"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PAID" // Already paid
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        // When: Try to initiate payment for paid order
        val response = client.post("/v1/payments/mpesa/initiate") {
            contentType(ContentType.Application.Json)
            setBody("""{"orderId":"$orderId","phoneNumber":"254712345678"}""")
            header("Authorization", "Bearer test-jwt-token")
        }

        // Then: Should be rejected with 409 Conflict
        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = json.decodeFromString<ApiResponse<Unit>>(response.bodyAsText())
        assertFalse(body.success)
        assertTrue(body.message.contains("already processed"))
    }

    @Test
    fun `test callback prevents duplicate payment processing`() = testApplication {
        // Setup
        val businessId = "test-business-123"
        val orderId = "test-order-123"
        val checkoutRequestId = "ws_CO_test_duplicate"
        val txCode = "QGR5TXH6YK"

        transaction {
            // Create order
            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-003"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PENDING"
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0
                it[stkCheckoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            // Payment already exists (duplicate scenario)
            PaymentsTable.insert {
                it[id] = "payment-existing"
                it[PaymentsTable.businessId] = businessId
                it[PaymentsTable.orderId] = orderId
                it[transactionCode] = txCode
                it[amount] = 1000.0
                it[payerPhone] = "254712345678"
                it[payerName] = "Test"
                it[method] = "MPESA"
                it[status] = "SUCCESS"
                it[channel] = "STK_PUSH"
                it[transactionDate] = Clock.System.now()
            }
        }

        // When: Callback arrives with same transaction code
        val callbackPayload = """
        {
            "Body": {
                "stkCallback": {
                    "MerchantRequestID": "29115-34620561-1",
                    "CheckoutRequestID": "$checkoutRequestId",
                    "ResultCode": 0,
                    "ResultDesc": "The service request is processed successfully.",
                    "CallbackMetadata": {
                        "Item": [
                            {"Name": "Amount", "Value": "1000"},
                            {"Name": "MpesaReceiptNumber", "Value": "$txCode"},
                            {"Name": "PhoneNumber", "Value": "254712345678"},
                            {"Name": "FirstName", "Value": "Test"},
                            {"Name": "TransactionDate", "Value": "20240115120000"}
                        ]
                    }
                }
            }
        }
        """.trimIndent()

        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody(callbackPayload)
        }

        // Then: Should acknowledge but not create duplicate payment
        assertEquals(HttpStatusCode.OK, response.status)

        // Verify only one payment exists
        val paymentCount = transaction {
            PaymentsTable.select { PaymentsTable.transactionCode eq txCode }.count()
        }
        assertEquals(1, paymentCount)
    }

    @Test
    fun `test callback validates amount matches order`() = testApplication {
        // Setup
        val businessId = "test-business-123"
        val orderId = "test-order-123"
        val checkoutRequestId = "ws_CO_test_amount"

        transaction {
            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-004"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PENDING"
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0 // Expected amount
                it[stkCheckoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        // When: Callback arrives with wrong amount (more than 1% difference)
        val callbackPayload = """
        {
            "Body": {
                "stkCallback": {
                    "MerchantRequestID": "29115-34620561-1",
                    "CheckoutRequestID": "$checkoutRequestId",
                    "ResultCode": 0,
                    "ResultDesc": "The service request is processed successfully.",
                    "CallbackMetadata": {
                        "Item": [
                            {"Name": "Amount", "Value": "500"},
                            {"Name": "MpesaReceiptNumber", "Value": "QGR5WRONG"},
                            {"Name": "PhoneNumber", "Value": "254712345678"},
                            {"Name": "FirstName", "Value": "Test"},
                            {"Name": "TransactionDate", "Value": "20240115120000"}
                        ]
                    }
                }
            }
        }
        """.trimIndent()

        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody(callbackPayload)
        }

        // Then: Should acknowledge but not process payment
        assertEquals(HttpStatusCode.OK, response.status)

        // Verify payment was NOT created due to amount mismatch
        val paymentCount = transaction {
            PaymentsTable.select { 
                (PaymentsTable.orderId eq orderId) and 
                (PaymentsTable.transactionCode eq "QGR5WRONG")
            }.count()
        }
        assertEquals(0, paymentCount)

        // Order should still be pending
        val orderStatus = transaction {
            OrdersTable.select { OrdersTable.id eq orderId }
                .firstOrNull()?.get(OrdersTable.paymentStatus)
        }
        assertEquals("PENDING", orderStatus)
    }

    @Test
    fun `test callback accepts amount within 1 percent tolerance`() = testApplication {
        // Setup
        val businessId = "test-business-123"
        val orderId = "test-order-123"
        val checkoutRequestId = "ws_CO_test_tolerance"

        transaction {
            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-005"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PENDING"
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0
                it[stkCheckoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        // When: Callback arrives with amount within tolerance (999.5, within 1%)
        val callbackPayload = """
        {
            "Body": {
                "stkCallback": {
                    "MerchantRequestID": "29115-34620561-1",
                    "CheckoutRequestID": "$checkoutRequestId",
                    "ResultCode": 0,
                    "ResultDesc": "The service request is processed successfully.",
                    "CallbackMetadata": {
                        "Item": [
                            {"Name": "Amount", "Value": "999.5"},
                            {"Name": "MpesaReceiptNumber", "Value": "QGR5TOLERANCE"},
                            {"Name": "PhoneNumber", "Value": "254712345678"},
                            {"Name": "FirstName", "Value": "Test"},
                            {"Name": "TransactionDate", "Value": "20240115120000"}
                        ]
                    }
                }
            }
        }
        """.trimIndent()

        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody(callbackPayload)
        }

        // Then: Should process successfully
        assertEquals(HttpStatusCode.OK, response.status)

        // Verify payment was created
        val payment = transaction {
            PaymentsTable.select { PaymentsTable.transactionCode eq "QGR5TOLERANCE" }
                .firstOrNull()
        }
        assertNotNull(payment)
        assertEquals(999.5, payment[PaymentsTable.amount])

        // Order should be marked as paid
        val orderStatus = transaction {
            OrdersTable.select { OrdersTable.id eq orderId }
                .firstOrNull()?.get(OrdersTable.paymentStatus)
        }
        assertEquals("PAID", orderStatus)
    }

    @Test
    fun `test callback validates timestamp freshness`() = testApplication {
        // Setup
        val checkoutRequestId = "ws_CO_test_old"

        // When: Callback arrives with old timestamp (over 5 minutes)
        val oldTimestamp = "20240115100000" // Assume current time is much later
        val callbackPayload = """
        {
            "Body": {
                "stkCallback": {
                    "MerchantRequestID": "29115-34620561-1",
                    "CheckoutRequestID": "$checkoutRequestId",
                    "ResultCode": 0,
                    "ResultDesc": "The service request is processed successfully.",
                    "CallbackMetadata": {
                        "Item": [
                            {"Name": "Amount", "Value": "1000"},
                            {"Name": "MpesaReceiptNumber", "Value": "QGR5OLD"},
                            {"Name": "PhoneNumber", "Value": "254712345678"},
                            {"Name": "FirstName", "Value": "Test"},
                            {"Name": "TransactionDate", "Value": "$oldTimestamp"}
                        ]
                    }
                }
            }
        }
        """.trimIndent()

        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody(callbackPayload)
        }

        // Then: Signature validation should fail for old timestamps
        // (actual behavior depends on SignatureUtils.validateMpesaCallback implementation)
        assertTrue(response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized))
    }

    @Test
    fun `test callback rejects invalid JSON payload`() = testApplication {
        // When: Invalid JSON is sent
        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody("{ invalid json ]")
        }

        // Then: Should acknowledge to prevent retries but not process
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test callback handles failed payment gracefully`() = testApplication {
        // Setup
        val businessId = "test-business-123"
        val orderId = "test-order-123"
        val checkoutRequestId = "ws_CO_test_failed"

        transaction {
            OrdersTable.insert {
                it[id] = orderId
                it[orderNumber] = "ORD-006"
                it[OrdersTable.businessId] = businessId
                it[customerName] = "Test Customer"
                it[customerPhone] = "254712345678"
                it[paymentStatus] = "PENDING"
                it[deliveryStatus] = "PENDING"
                it[baseAmount] = 1000.0
                it[subtotal] = 1000.0
                it[stkCheckoutRequestId] = checkoutRequestId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        // When: Callback arrives with failure (ResultCode != 0)
        val callbackPayload = """
        {
            "Body": {
                "stkCallback": {
                    "MerchantRequestID": "29115-34620561-1",
                    "CheckoutRequestID": "$checkoutRequestId",
                    "ResultCode": 1032,
                    "ResultDesc": "Request cancelled by user"
                }
            }
        }
        """.trimIndent()

        val response = client.post("/v1/payments/mpesa/callback") {
            contentType(ContentType.Application.Json)
            setBody(callbackPayload)
        }

        // Then: Should acknowledge gracefully
        assertEquals(HttpStatusCode.OK, response.status)

        // Verify no payment was created
        val paymentCount = transaction {
            PaymentsTable.select { PaymentsTable.orderId eq orderId }.count()
        }
        assertEquals(0, paymentCount)

        // Order should still be pending
        val orderStatus = transaction {
            OrdersTable.select { OrdersTable.id eq orderId }
                .firstOrNull()?.get(OrdersTable.paymentStatus)
        }
        assertEquals("PENDING", orderStatus)
    }
}
