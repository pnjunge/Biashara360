package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val orderNumber: String,       // Auto-generated e.g. B360-0001
    val businessId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String = "",
    val items: List<OrderItem>,
    val paymentStatus: PaymentStatus,
    val deliveryStatus: DeliveryStatus,
    val paymentMethod: PaymentMethod = PaymentMethod.MPESA,
    val mpesaTransactionCode: String? = null,
    val includeTax: Boolean = false,
    val taxRate: Double = 0.16,
    val notes: String = "",
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val subtotal: Double get() = items.sumOf { it.quantity * it.unitPrice }
    val taxAmount: Double get() = if (includeTax) kotlin.math.round(subtotal * taxRate * 100.0) / 100.0 else 0.0
    val total: Double get() = subtotal + taxAmount
    val totalItems: Int get() = items.sumOf { it.quantity }
}

@Serializable
data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val buyingPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
    val lineProfit: Double get() = quantity * (unitPrice - buyingPrice)
}

@Serializable
enum class PaymentStatus {
    PAID, PENDING, PROCESSING, COMPLETED, COD, FAILED, REFUNDED, CANCELLED, VOIDED;

    companion object {
        fun fromString(value: String?): PaymentStatus {
            if (value.isNullOrBlank()) return PENDING
            val upper = value.trim().uppercase()
            return entries.find { it.name == upper } ?: when (upper) {
                "COMPLETED" -> PAID
                "CANCELED" -> CANCELLED
                else -> PENDING
            }
        }
    }

    fun displayLabel(): String = when (this) {
        PAID, COMPLETED -> "Paid"
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        COD -> "Cash on Delivery"
        FAILED -> "Failed"
        REFUNDED -> "Refunded"
        CANCELLED -> "Cancelled"
        VOIDED -> "Voided"
    }
}

@Serializable
enum class DeliveryStatus {
    PENDING, PROCESSING, PREPARING, READY_FOR_PICKUP, IN_TRANSIT, OUT_FOR_DELIVERY, SHIPPED, DELIVERED, CANCELLED, FAILED, RETURNED;

    companion object {
        fun fromString(value: String?): DeliveryStatus {
            if (value.isNullOrBlank()) return PENDING
            val upper = value.trim().uppercase()
            return entries.find { it.name == upper } ?: when (upper) {
                "CANCELED" -> CANCELLED
                "PREPARING", "READY_FOR_PICKUP", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> PROCESSING
                else -> PENDING
            }
        }
    }

    fun displayLabel(): String = when (this) {
        PENDING -> "Pending"
        PROCESSING, PREPARING, READY_FOR_PICKUP, IN_TRANSIT, OUT_FOR_DELIVERY -> "Processing"
        SHIPPED -> "Shipped"
        DELIVERED -> "Delivered"
        CANCELLED -> "Cancelled"
        FAILED -> "Failed"
        RETURNED -> "Returned"
    }
}

@Serializable
enum class PaymentMethod {
    MPESA, AIRTEL_MONEY, TKASH, CASH, COD, CARD, BANK_TRANSFER, CREDIT;

    companion object {
        fun fromString(value: String?): PaymentMethod {
            if (value.isNullOrBlank()) return MPESA
            val upper = value.trim().uppercase()
            return entries.find { it.name == upper } ?: MPESA
        }
    }
}
