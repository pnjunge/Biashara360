package com.app.biashara.dto

import kotlinx.serialization.Serializable

/**
 * Request/Response DTOs for Order endpoints.
 */

// ──── Create Order ───────────────────────────────────────────────────────────

@Serializable
data class CreateOrderRequestDTO(
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String = "",
    val items: List<OrderItemRequestDTO>,
    val paymentMethod: String = "MPESA",  // CASH, MPESA, CARD, BANK_TRANSFER
    val notes: String = "",
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0
)

@Serializable
data class OrderItemRequestDTO(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double = 0.0
)

// ──── Order Response ─────────────────────────────────────────────────────────

@Serializable
data class OrderDTO(
    val id: String,
    val orderNumber: String,
    val businessId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String,
    val items: List<OrderItemDTO>,
    val paymentStatus: String,
    val deliveryStatus: String,
    val paymentMethod: String,
    val mpesaTransactionCode: String? = null,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val notes: String,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String? = null
)

@Serializable
data class OrderItemDTO(
    val id: String,
    val productId: String,
    val productName: String,
    val sku: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val buyingPrice: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val lineTotal: Double,
    val lineProfit: Double
)

@Serializable
data class OrderSummaryDTO(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val totalAmount: Double,
    val paymentStatus: String,
    val deliveryStatus: String,
    val createdAt: String
)

// ──── Update Order Status ────────────────────────────────────────────────────

@Serializable
data class UpdatePaymentStatusRequestDTO(
    val status: String,  // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    val mpesaTransactionCode: String? = null,
    val paymentNotes: String? = null
)

@Serializable
data class UpdateDeliveryStatusRequestDTO(
    val status: String,  // PENDING, PREPARING, IN_TRANSIT, DELIVERED, FAILED
    val deliveryNotes: String? = null
)

@Serializable
data class CancelOrderRequestDTO(
    val reason: String,
    val refundPayment: Boolean = false
)

// ──── Order Filters & Search ─────────────────────────────────────────────────

@Serializable
data class OrderListQueryDTO(
    val paymentStatus: String? = null,
    val deliveryStatus: String? = null,
    val customerId: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val searchQuery: String? = null,  // order number or customer name
    val page: Int = 1,
    val pageSize: Int = 20
)

// ──── Order Analytics ────────────────────────────────────────────────────────

@Serializable
data class OrderStatsDTO(
    val totalOrders: Int,
    val totalRevenue: Double,
    val totalProfit: Double,
    val averageOrderValue: Double,
    val pendingOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int
)
