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
    val notes: String = "",
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val subtotal: Double get() = items.sumOf { it.quantity * it.unitPrice }
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
    PAID, PENDING, COD, FAILED, REFUNDED;

    fun displayLabel(): String = when (this) {
        PAID -> "Paid"
        PENDING -> "Pending"
        COD -> "Cash on Delivery"
        FAILED -> "Failed"
        REFUNDED -> "Refunded"
    }
}

@Serializable
enum class DeliveryStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED;

    fun displayLabel(): String = when (this) {
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        SHIPPED -> "Shipped"
        DELIVERED -> "Delivered"
        CANCELLED -> "Cancelled"
    }
}

@Serializable
enum class PaymentMethod {
    MPESA, AIRTEL_MONEY, TKASH, CASH, CARD, BANK_TRANSFER
}
