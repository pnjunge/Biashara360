package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: String,
    val businessId: String,
    val orderId: String?,
    val transactionCode: String,    // Mpesa transaction code e.g. RGK71H...
    val amount: Double,
    val payerPhone: String,
    val payerName: String,
    val method: PaymentMethod,
    val status: TransactionStatus,
    val channel: PaymentChannel,
    val reconciled: Boolean = false,
    val notes: String = "",
    val transactionDate: Instant
)

@Serializable
enum class TransactionStatus {
    SUCCESS, PENDING, FAILED, CANCELLED, REVERSED
}

@Serializable
enum class PaymentChannel {
    MPESA_STK_PUSH,
    MPESA_C2B,          // Customer pays paybill/till directly
    MPESA_SEND_MONEY,   // Manual reconciliation
    AIRTEL_MONEY,
    TKASH,
    CASH,
    CARD_VISA,
    CARD_MASTERCARD,
    BANK_TRANSFER
}

// Mpesa STK Push Request
@Serializable
data class MpesaStkPushRequest(
    val businessShortCode: String,
    val phoneNumber: String,       // Customer phone in format 2547XXXXXXXX
    val amount: Double,
    val accountReference: String,  // Order number
    val transactionDesc: String
)

@Serializable
data class MpesaStkPushResponse(
    val merchantRequestId: String,
    val checkoutRequestId: String,
    val responseCode: String,
    val responseDescription: String,
    val customerMessage: String
)

// Dashboard summary
@Serializable
data class PaymentDashboard(
    val businessId: String,
    val totalCollected: Double,
    val pendingAmount: Double,
    val transactionCount: Int,
    val byChannel: Map<String, Double>,
    val recentTransactions: List<Payment>
)
