package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ProductService(
    private val auditLogService: AuditLogService? = null
) {

    fun getAll(businessId: String, query: String? = null, lowStockOnly: Boolean = false, includeInactive: Boolean = false): List<ProductResponse> = transaction {
        var stmt = if (includeInactive) {
            ProductsTable.select { ProductsTable.businessId eq businessId }
        } else {
            ProductsTable.select { (ProductsTable.businessId eq businessId) and (ProductsTable.isActive eq true) }
        }
        if (!query.isNullOrBlank()) {
            stmt = stmt.andWhere {
                (ProductsTable.name.lowerCase() like "%${query.lowercase()}%") or
                (ProductsTable.sku.lowerCase() like "%${query.lowercase()}%")
            }
        }
        val rows = stmt.orderBy(ProductsTable.name).toList()
        val recipes = recipeStock(businessId, rows.map { it[ProductsTable.id] })
        rows.map { it.toResponse(recipes[it[ProductsTable.id]]) }.filter { !lowStockOnly || it.currentStock <= it.lowStockThreshold }
    }

    fun toggleStatus(id: String, businessId: String, isActive: Boolean): ApiResponse<ProductResponse> = transaction {
        val updated = ProductsTable.update({
            (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)
        }) {
            it[ProductsTable.isActive] = isActive
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) ApiResponse(false, message = "Product not found")
        else {
            auditLogService?.logEvent(businessId, null, null, if (isActive) "ENABLE_PRODUCT" else "DISABLE_PRODUCT", null, "Product $id status changed to isActive=$isActive")
            val product = ProductsTable.select { ProductsTable.id eq id }.first().toResponse()
            ApiResponse(true, data = product, message = if (isActive) "Product enabled" else "Product disabled")
        }
    }

    fun getById(id: String, businessId: String): ProductResponse? = transaction {
        ProductsTable.select { (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId) }
            .firstOrNull()?.toResponse()
    }

    fun create(businessId: String, req: ProductRequest): ApiResponse<ProductResponse> = transaction {
        val existing = ProductsTable.select {
            (ProductsTable.businessId eq businessId) and
                ((ProductsTable.sku eq req.sku) or
                    (req.barcode?.let { ProductsTable.barcode eq it } ?: Op.FALSE))
        }.firstOrNull()
        if (existing != null) return@transaction ApiResponse(false, message = "SKU or barcode already exists")

        val id = generateId()
        val now = Clock.System.now()
        ProductsTable.insert {
            it[ProductsTable.id] = id
            it[ProductsTable.businessId] = businessId
            it[sku] = req.sku
            it[name] = req.name
            it[description] = req.description
            it[buyingPrice] = req.buyingPrice
            it[sellingPrice] = req.sellingPrice
            it[currentStock] = req.currentStock
            it[lowStockThreshold] = req.lowStockThreshold
            it[category] = req.category
            it[barcode] = req.barcode?.trim()?.ifBlank { null }
            it[imageUrl] = req.imageUrl
            it[createdAt] = now
            it[updatedAt] = now
        }
        auditLogService?.logEvent(businessId, null, null, "CREATE_PRODUCT", null, "Created product ${req.name} (SKU: ${req.sku})")
        val product = ProductsTable.select { ProductsTable.id eq id }.first().toResponse()
        ApiResponse(true, data = product, message = "Product created")
    }

    fun update(id: String, businessId: String, req: ProductRequest): ApiResponse<ProductResponse> = transaction {
        val current = ProductsTable.select {
            (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Product not found")
        if (req.expectedUpdatedAt != null && current[ProductsTable.updatedAt].toString() != req.expectedUpdatedAt) {
            return@transaction ApiResponse(
                false,
                message = "This product changed since you opened it. Refresh and review the latest values."
            )
        }
        val duplicate = ProductsTable.select {
            (ProductsTable.businessId eq businessId) and
                (ProductsTable.id neq id) and
                ((ProductsTable.sku eq req.sku) or
                    (req.barcode?.let { ProductsTable.barcode eq it } ?: Op.FALSE))
        }.any()
        if (duplicate) return@transaction ApiResponse(false, message = "SKU or barcode already exists")
        val previousStock = current[ProductsTable.currentStock]
        val updated = ProductsTable.update({
            (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)
        }) {
            it[sku] = req.sku
            it[name] = req.name
            it[description] = req.description
            it[buyingPrice] = req.buyingPrice
            it[sellingPrice] = req.sellingPrice
            if (recipeStock(businessId, listOf(id))[id] == null) it[currentStock] = req.currentStock
            it[lowStockThreshold] = req.lowStockThreshold
            it[category] = req.category
            it[barcode] = req.barcode?.trim()?.ifBlank { null }
            it[imageUrl] = req.imageUrl
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Product not found")
        if (recipeStock(businessId, listOf(id))[id] == null && req.currentStock != previousStock) {
            val diff = req.currentStock - previousStock
            StockMovementsTable.insert {
                it[StockMovementsTable.id] = generateId()
                it[StockMovementsTable.productId] = id
                it[StockMovementsTable.businessId] = businessId
                it[StockMovementsTable.type] = if (diff > 0) "STOCK_IN" else if (diff < 0) "STOCK_OUT" else "ADJUSTMENT"
                it[StockMovementsTable.quantity] = kotlin.math.abs(diff)
                it[StockMovementsTable.note] = "Product update adjustment"
                it[StockMovementsTable.recordedAt] = Clock.System.now()
            }
        }
        auditLogService?.logEvent(businessId, null, null, "UPDATE_PRODUCT", null, "Updated product ${req.name} (SKU: ${req.sku})")
        val product = ProductsTable.select { ProductsTable.id eq id }.first().toResponse()
        ApiResponse(true, data = product)
    }

    fun updateStock(productId: String, businessId: String, req: StockUpdateRequest): ApiResponse<ProductResponse> = transaction {
        val product = ProductsTable.select {
            (ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Product not found")

        if (recipeStock(businessId, listOf(productId))[productId] != null)
            return@transaction ApiResponse(false, message = "This item uses bulk ingredients. Receive or adjust ingredient stock in Hospitality Operations.")
        val currentStock = product[ProductsTable.currentStock]
        val newStock = when (req.type) {
            "STOCK_IN" -> currentStock + req.quantity
            "STOCK_OUT" -> maxOf(0, currentStock - req.quantity)
            "ADJUSTMENT" -> req.quantity
            else -> return@transaction ApiResponse(false, message = "Invalid movement type")
        }

        ProductsTable.update({ ProductsTable.id eq productId }) {
            it[ProductsTable.currentStock] = newStock
            it[updatedAt] = Clock.System.now()
        }

        StockMovementsTable.insert {
            it[id] = generateId()
            it[StockMovementsTable.productId] = productId
            it[StockMovementsTable.businessId] = businessId
            it[type] = req.type
            it[quantity] = req.quantity
            it[note] = req.note ?: ""
            it[recordedAt] = Clock.System.now()
        }

        auditLogService?.logEvent(businessId, null, null, "UPDATE_STOCK", null, "Stock updated for ${product[ProductsTable.name]} ($currentStock -> $newStock, type: ${req.type})")
        val updated = ProductsTable.select { ProductsTable.id eq productId }.first().toResponse()
        ApiResponse(true, data = updated, message = "Stock updated")
    }

    fun delete(id: String, businessId: String): ApiResponse<Unit> = transaction {
        val updated = ProductsTable.update({
            (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)
        }) {
            it[isActive] = false
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) ApiResponse(false, message = "Product not found")
        else {
            auditLogService?.logEvent(businessId, null, null, "DELETE_PRODUCT", null, "Deactivated/deleted product $id")
            ApiResponse(true, message = "Product deleted")
        }
    }

    private fun ResultRow.toResponse(recipe: RecipeStock? = recipeStock(this[ProductsTable.businessId], listOf(this[ProductsTable.id]))[this[ProductsTable.id]]): ProductResponse {
        val buying = recipe?.cost ?: this[ProductsTable.buyingPrice]
        val selling = this[ProductsTable.sellingPrice]
        val stock = recipe?.available ?: this[ProductsTable.currentStock]
        val threshold = this[ProductsTable.lowStockThreshold]
        val profit = selling - buying
        return ProductResponse(
            stockMode = if (recipe != null) "INGREDIENTS" else "PRODUCT",
            productStock = this[ProductsTable.currentStock],
            id = this[ProductsTable.id],
            businessId = this[ProductsTable.businessId],
            sku = this[ProductsTable.sku],
            name = this[ProductsTable.name],
            description = this[ProductsTable.description],
            buyingPrice = buying,
            sellingPrice = selling,
            profitPerItem = profit,
            profitMargin = if (selling > 0) (profit / selling) * 100 else 0.0,
            currentStock = stock,
            lowStockThreshold = threshold,
            isLowStock = stock in 1..threshold,
            isOutOfStock = stock <= 0,
            category = this[ProductsTable.category],
            barcode = this[ProductsTable.barcode],
            imageUrl = this[ProductsTable.imageUrl],
            isActive = this[ProductsTable.isActive],
            createdAt = this[ProductsTable.createdAt].toString(),
            updatedAt = this[ProductsTable.updatedAt].toString()
        )
    }
}
