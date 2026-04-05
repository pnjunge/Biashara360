package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val businessId: String,
    val sku: String,          // SID in BRD
    val name: String,
    val description: String = "",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int,
    val lowStockThreshold: Int = 5,
    val category: String = "",
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val profitPerItem: Double get() = sellingPrice - buyingPrice
    val profitMargin: Double get() = if (sellingPrice > 0) (profitPerItem / sellingPrice) * 100 else 0.0
    val isLowStock: Boolean get() = currentStock <= lowStockThreshold
    val isOutOfStock: Boolean get() = currentStock <= 0
}

@Serializable
data class StockMovement(
    val id: String,
    val productId: String,
    val businessId: String,
    val type: StockMovementType,
    val quantity: Int,
    val note: String = "",
    val orderId: String? = null,
    val recordedAt: Instant
)

@Serializable
enum class StockMovementType {
    STOCK_IN,    // Restocking
    STOCK_OUT,   // Sale or damage
    ADJUSTMENT   // Manual correction
}
