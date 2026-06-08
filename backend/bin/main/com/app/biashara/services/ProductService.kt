package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ProductService {

    fun getAll(businessId: String, query: String? = null, lowStockOnly: Boolean = false): List<ProductResponse> = transaction {
        var stmt = ProductsTable.select { (ProductsTable.businessId eq businessId) and (ProductsTable.isActive eq true) }
        if (!query.isNullOrBlank()) {
            stmt = stmt.andWhere {
                (ProductsTable.name.lowerCase() like "%${query.lowercase()}%") or
                (ProductsTable.sku.lowerCase() like "%${query.lowercase()}%")
            }
        }
        if (lowStockOnly) {
            stmt = stmt.andWhere { ProductsTable.currentStock lessEq ProductsTable.lowStockThreshold }
        }
        stmt.orderBy(ProductsTable.name).map { it.toResponse() }
    }

    fun getById(id: String, businessId: String): ProductResponse? = transaction {
        ProductsTable.select { (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId) }
            .firstOrNull()?.toResponse()
    }

    fun create(businessId: String, req: ProductRequest): ApiResponse<ProductResponse> = transaction {
        val existing = ProductsTable.select {
            (ProductsTable.businessId eq businessId) and (ProductsTable.sku eq req.sku)
        }.firstOrNull()
        if (existing != null) return@transaction ApiResponse(false, message = "SKU already exists")

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
            it[imageUrl] = req.imageUrl
            it[createdAt] = now
            it[updatedAt] = now
        }
        val product = ProductsTable.select { ProductsTable.id eq id }.first().toResponse()
        ApiResponse(true, data = product, message = "Product created")
    }

    fun update(id: String, businessId: String, req: ProductRequest): ApiResponse<ProductResponse> = transaction {
        val updated = ProductsTable.update({
            (ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)
        }) {
            it[sku] = req.sku
            it[name] = req.name
            it[description] = req.description
            it[buyingPrice] = req.buyingPrice
            it[sellingPrice] = req.sellingPrice
            it[lowStockThreshold] = req.lowStockThreshold
            it[category] = req.category
            it[imageUrl] = req.imageUrl
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Product not found")
        val product = ProductsTable.select { ProductsTable.id eq id }.first().toResponse()
        ApiResponse(true, data = product)
    }

    fun updateStock(productId: String, businessId: String, req: StockUpdateRequest): ApiResponse<ProductResponse> = transaction {
        val product = ProductsTable.select {
            (ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Product not found")

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
            it[note] = req.note
            it[recordedAt] = Clock.System.now()
        }

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
        else ApiResponse(true, message = "Product deleted")
    }

    private fun ResultRow.toResponse(): ProductResponse {
        val buying = this[ProductsTable.buyingPrice]
        val selling = this[ProductsTable.sellingPrice]
        val stock = this[ProductsTable.currentStock]
        val threshold = this[ProductsTable.lowStockThreshold]
        val profit = selling - buying
        return ProductResponse(
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
            imageUrl = this[ProductsTable.imageUrl],
            createdAt = this[ProductsTable.createdAt].toString(),
            updatedAt = this[ProductsTable.updatedAt].toString()
        )
    }
}
