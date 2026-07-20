package com.app.biashara.dto

import kotlinx.serialization.Serializable

/**
 * Request/Response DTOs for Payment endpoints (Mpesa, CyberSource).
 */

// ──── Mpesa STK Push ─────────────────────────────────────────────────────────

@Serializable
data class InitiatePaymentRequestDTO(
    val orderId: String,
    val phoneNumber: String,
    val amount: Double? = null  // Optional: defaults to order total
)

@Serializable
data class StkPushResponseDTO(
    val merchantRequestId: String,
    val checkoutRequestId: String,
    val responseCode: String,
    val responseDescription: String,
    val customerMessage: String
)

@Serializable
data class PaymentStatusDTO(
    val orderId: String,
    val status: String,
    val transactionCode: String? = null,
    val amount: Double,
    val phoneNumber: String,
    val timestamp: String
)

// ──── Payment Query ──────────────────────────────────────────────────────────

@Serializable
data class CheckPaymentStatusRequestDTO(
    val checkoutRequestId: String
)

@Serializable
data class ReconcilePaymentRequestDTO(
    val orderId: String
)

// ──── CyberSource Card Payment ───────────────────────────────────────────────

@Serializable
data class InitiateCardPaymentRequestDTO(
    val orderId: String,
    val amount: Double? = null,
    val currency: String = "KES",
    val returnUrl: String? = null
)

@Serializable
data class CaptureContextResponseDTO(
    val keyId: String,
    val captureContext: String,  // JWT token for Flex Microform
    val expiresAt: String
)

@Serializable
data class ProcessCardPaymentRequestDTO(
    val orderId: String,
    val transientToken: String,  // From CyberSource Flex
    val cardholderName: String,
    val saveCard: Boolean = false
)

@Serializable
data class CardPaymentResponseDTO(
    val transactionId: String,
    val status: String,
    val approvalCode: String? = null,
    val reconciliationId: String? = null,
    val cardLast4: String? = null,
    val cardType: String? = null,
    val message: String
)

// ──── Payment History ────────────────────────────────────────────────────────

@Serializable
data class PaymentDTO(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val amount: Double,
    val paymentMethod: String,
    val status: String,
    val transactionCode: String? = null,
    val phoneNumber: String? = null,
    val cardLast4: String? = null,
    val createdAt: String,
    val completedAt: String? = null
)

@Serializable
data class PaymentListQueryDTO(
    val orderId: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

// ──── Refund ─────────────────────────────────────────────────────────────────

@Serializable
data class InitiateRefundRequestDTO(
    val orderId: String,
    val amount: Double,
    val reason: String
)

@Serializable
data class RefundResponseDTO(
    val refundId: String,
    val status: String,
    val message: String
)
