package com.app.biashara.dto

import kotlinx.serialization.Serializable

/**
 * Request/Response DTOs for Product/Inventory endpoints.
 */

// ──── Product Request ────────────────────────────────────────────────────────

@Serializable
data class CreateProductRequestDTO(
    val sku: String,
    val name: String,
    val description: String = "",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int = 0,
    val lowStockThreshold: Int = 5,
    val category: String = "OTHER",
    val imageUrl: String? = null,
    val barcode: String? = null,
    val taxable: Boolean = true
)

@Serializable
data class UpdateProductRequestDTO(
    val sku: String? = null,
    val name: String? = null,
    val description: String? = null,
    val buyingPrice: Double? = null,
    val sellingPrice: Double? = null,
    val lowStockThreshold: Int? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val barcode: String? = null,
    val taxable: Boolean? = null
)

// ──── Product Response ───────────────────────────────────────────────────────

@Serializable
data class ProductDTO(
    val id: String,
    val businessId: String,
    val sku: String,
    val name: String,
    val description: String,
    val buyingPrice: Double,
    val sellingPrice: Double,
    val profitPerItem: Double,
    val profitMargin: Double,
    val currentStock: Int,
    val lowStockThreshold: Int,
    val isLowStock: Boolean,
    val isOutOfStock: Boolean,
    val category: String,
    val imageUrl: String?,
    val barcode: String? = null,
    val taxable: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProductSummaryDTO(
    val id: String,
    val sku: String,
    val name: String,
    val sellingPrice: Double,
    val currentStock: Int,
    val category: String
)

// ──── Stock Management ───────────────────────────────────────────────────────

@Serializable
data class StockUpdateRequestDTO(
    val type: String,  // STOCK_IN, STOCK_OUT, ADJUSTMENT, RETURN, DAMAGE
    val quantity: Int,
    val note: String = "",
    val referenceNumber: String? = null
)

@Serializable
data class StockMovementDTO(
    val id: String,
    val productId: String,
    val productName: String,
    val type: String,
    val quantity: Int,
    val note: String,
    val orderId: String? = null,
    val recordedBy: String? = null,
    val recordedAt: String
)

@Serializable
data class BulkStockUpdateRequestDTO(
    val items: List<BulkStockItemDTO>
)

@Serializable
data class BulkStockItemDTO(
    val productId: String,
    val quantity: Int,
    val note: String = ""
)

// ──── Product List with Filters ──────────────────────────────────────────────

@Serializable
data class ProductListQueryDTO(
    val query: String? = null,
    val category: String? = null,
    val lowStockOnly: Boolean = false,
    val outOfStockOnly: Boolean = false,
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String = "name",  // name, sku, stock, price
    val sortOrder: String = "asc"  // asc, desc
)
