package com.app.biashara.data.repository

import com.app.biashara.UserSession
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.ProductEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.ProductRepository
import com.app.biashara.domain.usecase.generateId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@kotlinx.serialization.Serializable
data class ProductDto(
    val id: String,
    val businessId: String,
    val sku: String,
    val name: String,
    val description: String = "",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int,
    val lowStockThreshold: Int = 5,
    val category: String = "",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class StockUpdateRequest(
    val type: String,
    val quantity: Int,
    val note: String = ""
)

@kotlinx.serialization.Serializable
private data class ProductRequestDto(
    val sku: String,
    val name: String,
    val description: String,
    val buyingPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int,
    val lowStockThreshold: Int,
    val category: String,
    val imageUrl: String?,
    val barcode: String?,
    val expectedUpdatedAt: String?
)

class ProductRepositoryImpl(
    private val database: Biashara360Database,
    private val client: HttpClient
) : ProductRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getProducts(businessId: String): Flow<List<Product>> {
        val effectiveId = businessId.ifBlank { UserSession.getBusinessId() }
        return queries.selectAllProducts(effectiveId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getLowStockProducts(businessId: String): Flow<List<Product>> {
        val effectiveId = businessId.ifBlank { UserSession.getBusinessId() }
        return queries.selectLowStockProducts(effectiveId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getProduct(id: String): Product? =
        queries.selectProductById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun saveProduct(product: Product): Result<Product> = runCatching {
        val request = ProductRequestDto(
            sku = product.sku,
            name = product.name,
            description = product.description,
            buyingPrice = product.buyingPrice,
            sellingPrice = product.sellingPrice,
            currentStock = product.currentStock,
            lowStockThreshold = product.lowStockThreshold,
            category = product.category,
            imageUrl = product.imageUrl,
            barcode = product.barcode,
            expectedUpdatedAt = null
        )
        var response: ApiResponse<ProductDto>? = runCatching {
            client.put("$BASE_URL/products/${product.id}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ApiResponse<ProductDto>>()
        }.getOrNull()

        val finalResponse: ApiResponse<ProductDto> = if (response != null && response.success && response.data != null) {
            response
        } else {
            client.post("$BASE_URL/products") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

        if (!finalResponse.success || finalResponse.data == null) {
            val details = finalResponse.errors.filter { it.isNotBlank() }.joinToString("; ")
            throw Exception(
                listOf(finalResponse.message, details)
                    .filter { it.isNotBlank() }
                    .joinToString(": ")
                    .ifBlank { "Failed to save product on backend" }
            )
        }

        var savedDto = requireNotNull(finalResponse.data)
        if (savedDto.currentStock != product.currentStock) {
            val stockResponse: ApiResponse<ProductDto> = client.post("$BASE_URL/products/${savedDto.id}/stock") {
                contentType(ContentType.Application.Json)
                setBody(StockUpdateRequest("ADJUSTMENT", product.currentStock, "Desktop inventory adjustment"))
            }.body()
            if (stockResponse.success && stockResponse.data != null) {
                savedDto = stockResponse.data
            }
        }

        val now = Clock.System.now().toString()
        if (savedDto.id != product.id) {
            queries.deleteProduct(now, product.id)
        }
        queries.insertProduct(
            id = savedDto.id,
            business_id = savedDto.businessId,
            sku = savedDto.sku,
            name = savedDto.name,
            description = savedDto.description,
            buying_price = savedDto.buyingPrice,
            selling_price = savedDto.sellingPrice,
            current_stock = savedDto.currentStock.toLong(),
            low_stock_threshold = savedDto.lowStockThreshold.toLong(),
            category = savedDto.category,
            barcode = savedDto.barcode,
            image_url = savedDto.imageUrl,
            is_active = if (savedDto.isActive) 1L else 0L,
            created_at = savedDto.createdAt,
            updated_at = savedDto.updatedAt
        )
        savedDto.toDomain()
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

        // Order create/cancel endpoints already adjust server inventory
        // transactionally. Only mirror those movements in the local cache.
        if (movement.orderId != null) return@runCatching

        val response: ApiResponse<ProductDto> = client.post("$BASE_URL/products/$productId/stock") {
            contentType(ContentType.Application.Json)
            setBody(
                StockUpdateRequest(
                    type = movement.type.name,
                    quantity = movement.quantity,
                    note = movement.note
                )
            )
        }.body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to update inventory on server" })
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        val response: ApiResponse<Unit> = client.delete("$BASE_URL/products/$id").body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to delete product on backend" })
        }
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

    /** Sync products from API and update local cache **/
    suspend fun syncProductsFromApi(businessId: String): Result<List<Product>> = runCatching {
        val effectiveId = businessId.ifBlank { UserSession.getBusinessId() }
        val response: ApiResponse<List<ProductDto>> = client.get("$BASE_URL/products") {
            url {
                if (effectiveId.isNotBlank()) {
                    parameters.append("businessId", effectiveId)
                }
            }
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch products" })
        }

        // The backend is authoritative. Hide stale desktop-only records that
        // are no longer returned for this business.
        val remoteIds = response.data.map { it.id }.toSet()
        queries.selectAllProducts(effectiveId).executeAsList()
            .filter { it.id !in remoteIds }
            .forEach { queries.deleteProduct(Clock.System.now().toString(), it.id) }

        // Update local cache.
        response.data.forEach { dto ->
            queries.insertProduct(
                id = dto.id,
                business_id = dto.businessId,
                sku = dto.sku,
                name = dto.name,
                description = dto.description,
                buying_price = dto.buyingPrice,
                selling_price = dto.sellingPrice,
                current_stock = dto.currentStock.toLong(),
                low_stock_threshold = dto.lowStockThreshold.toLong(),
                category = dto.category,
                barcode = dto.barcode,
                image_url = dto.imageUrl,
                is_active = if (dto.isActive) 1L else 0L,
                created_at = dto.createdAt,
                updated_at = dto.updatedAt
            )
        }

        response.data.map { it.toDomain() }
    }

    private fun ProductDto.toDomain() = Product(
        id = id,
        businessId = businessId,
        sku = sku,
        name = name,
        description = description,
        buyingPrice = buyingPrice,
        sellingPrice = sellingPrice,
        currentStock = currentStock,
        lowStockThreshold = lowStockThreshold,
        category = category,
        barcode = barcode,
        imageUrl = imageUrl,
        isActive = isActive,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )

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
        barcode = barcode,
        imageUrl = image_url,
        isActive = is_active == 1L,
        createdAt = Instant.parse(created_at),
        updatedAt = Instant.parse(updated_at)
    )
}
