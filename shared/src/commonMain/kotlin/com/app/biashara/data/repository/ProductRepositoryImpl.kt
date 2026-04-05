package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.ProductEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.ProductRepository
import com.app.biashara.domain.usecase.generateId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ProductRepositoryImpl(
    private val database: Biashara360Database
) : ProductRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getProducts(businessId: String): Flow<List<Product>> =
        queries.selectAllProducts(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getLowStockProducts(businessId: String): Flow<List<Product>> =
        queries.selectLowStockProducts(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProduct(id: String): Product? =
        queries.selectProductById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun saveProduct(product: Product): Result<Product> = runCatching {
        val now = Clock.System.now().toString()
        queries.insertProduct(
            id = product.id,
            business_id = product.businessId,
            sku = product.sku,
            name = product.name,
            description = product.description,
            buying_price = product.buyingPrice,
            selling_price = product.sellingPrice,
            current_stock = product.currentStock.toLong(),
            low_stock_threshold = product.lowStockThreshold.toLong(),
            category = product.category,
            image_url = product.imageUrl,
            is_active = if (product.isActive) 1L else 0L,
            created_at = product.createdAt.toString(),
            updated_at = now
        )
        product
    }

    override suspend fun updateStock(productId: String, movement: StockMovement): Result<Unit> = runCatching {
        val product = queries.selectProductById(productId).executeAsOneOrNull()
            ?: throw IllegalStateException("Product not found")
        val newStock = when (movement.type) {
            StockMovementType.STOCK_IN -> product.current_stock + movement.quantity
            StockMovementType.STOCK_OUT -> maxOf(0L, product.current_stock - movement.quantity)
            StockMovementType.ADJUSTMENT -> movement.quantity.toLong()
        }
        queries.updateStock(
            newStock = newStock,
            updatedAt = Clock.System.now().toString(),
            productId = productId
        )
        queries.insertMovement(
            id = generateId(),
            product_id = movement.productId,
            business_id = movement.businessId,
            type = movement.type.name,
            quantity = movement.quantity.toLong(),
            note = movement.note,
            order_id = movement.orderId,
            recorded_at = movement.recordedAt.toString()
        )
    }

    override suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        queries.deleteProduct(Clock.System.now().toString(), id)
    }

    override fun searchProducts(businessId: String, query: String): Flow<List<Product>> =
        queries.searchProducts(businessId, query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getStockMovements(productId: String): Flow<List<StockMovement>> =
        queries.selectMovementsByProduct(productId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { e ->
                    StockMovement(
                        id = e.id,
                        productId = e.product_id,
                        businessId = e.business_id,
                        type = StockMovementType.valueOf(e.type),
                        quantity = e.quantity.toInt(),
                        note = e.note,
                        orderId = e.order_id,
                        recordedAt = Instant.parse(e.recorded_at)
                    )
                }
            }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        businessId = business_id,
        sku = sku,
        name = name,
        description = description,
        buyingPrice = buying_price,
        sellingPrice = selling_price,
        currentStock = current_stock.toInt(),
        lowStockThreshold = low_stock_threshold.toInt(),
        category = category,
        imageUrl = image_url,
        isActive = is_active == 1L,
        createdAt = Instant.parse(created_at),
        updatedAt = Instant.parse(updated_at)
    )
}
