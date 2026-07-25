package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.OrderEntity
import com.app.biashara.db.OrderItemEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.OrderRepository
import com.app.biashara.domain.usecase.generateId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

@kotlinx.serialization.Serializable
data class OrderItemDto(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val buyingPrice: Double
)

@kotlinx.serialization.Serializable
data class OrderDto(
    val id: String,
    val orderNumber: String,
    val businessId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String = "",
    val items: List<OrderItemDto>,
    val paymentStatus: String,
    val deliveryStatus: String,
    val paymentMethod: String = "MPESA",
    val mpesaTransactionCode: String? = null,
    val notes: String = "",
    val createdAt: String,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class CreateOrderRequestDto(
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String,
    val items: List<CreateOrderItemRequestDto>,
    val paymentMethod: String,
    val notes: String
)

@kotlinx.serialization.Serializable
private data class CreateOrderItemRequestDto(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double
)

@kotlinx.serialization.Serializable
private data class PagedOrdersDto(
    val data: List<OrderDto>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

@kotlinx.serialization.Serializable
private data class PaymentStatusRequestDto(
    val status: String,
    val mpesaTransactionCode: String? = null
)

@kotlinx.serialization.Serializable
private data class DeliveryStatusRequestDto(val status: String)

class OrderRepositoryImpl(
    private val database: Biashara360Database,
    private val client: HttpClient
) : OrderRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getOrders(businessId: String): Flow<List<Order>> =
        queries.selectAllOrders(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomainWithItems() } }

    override fun getOrdersByStatus(businessId: String, status: PaymentStatus): Flow<List<Order>> =
        queries.selectOrdersByStatus(businessId, status.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomainWithItems() } }

    override suspend fun getOrder(id: String): Order? =
        queries.selectOrderById(id).executeAsOneOrNull()?.toDomainWithItems()

    override suspend fun createOrder(order: Order): Result<Order> = runCatching {
        val response: ApiResponse<OrderDto> = client.post("$BASE_URL/orders") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateOrderRequestDto(
                    customerId = order.customerId,
                    customerName = order.customerName,
                    customerPhone = order.customerPhone,
                    deliveryLocation = order.deliveryLocation,
                    items = order.items.map { CreateOrderItemRequestDto(it.productId, it.quantity, it.unitPrice) },
                    paymentMethod = order.paymentMethod.name,
                    notes = order.notes
                )
            )
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to create order on backend" })
        }
        val saved = requireNotNull(response.data)
        queries.insertOrder(
            id = saved.id, order_number = saved.orderNumber, business_id = saved.businessId,
            customer_id = saved.customerId, customer_name = saved.customerName,
            customer_phone = saved.customerPhone, delivery_location = saved.deliveryLocation,
            payment_status = saved.paymentStatus, delivery_status = saved.deliveryStatus,
            payment_method = saved.paymentMethod, mpesa_transaction_code = saved.mpesaTransactionCode,
            notes = saved.notes, subtotal = saved.items.sumOf { it.quantity * it.unitPrice },
            created_at = saved.createdAt, updated_at = saved.updatedAt
        )
        saved.items.forEach { item ->
            queries.insertOrderItem(
                id = generateId(),
                order_id = saved.id,
                product_id = item.productId,
                product_name = item.productName,
                quantity = item.quantity.toLong(),
                unit_price = item.unitPrice,
                buying_price = item.buyingPrice
            )
        }
        saved.toDomain()
    }

    override suspend fun updateOrder(order: Order): Result<Order> = runCatching {
        val now = kotlinx.datetime.Clock.System.now().toString()
        queries.updateOrderPaymentStatus(
            status = order.paymentStatus.name,
            txCode = order.mpesaTransactionCode,
            updatedAt = now,
            orderId = order.id
        )
        order
    }

    override suspend fun updatePaymentStatus(
        orderId: String,
        status: PaymentStatus,
        txCode: String?
    ): Result<Unit> = runCatching {
        val response: ApiResponse<OrderDto> = client.patch("$BASE_URL/orders/$orderId/payment-status") {
            contentType(ContentType.Application.Json)
            setBody(PaymentStatusRequestDto(status.name, txCode))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Failed to update payment status on backend" })
        queries.updateOrderPaymentStatus(
            status = status.name,
            txCode = txCode,
            updatedAt = kotlinx.datetime.Clock.System.now().toString(),
            orderId = orderId
        )
    }

    override suspend fun updateDeliveryStatus(
        orderId: String,
        status: DeliveryStatus
    ): Result<Unit> = runCatching {
        val response: ApiResponse<OrderDto> = client.patch("$BASE_URL/orders/$orderId/delivery-status") {
            contentType(ContentType.Application.Json)
            setBody(DeliveryStatusRequestDto(status.name))
        }.body()
        if (!response.success) throw Exception(response.message.ifBlank { "Failed to update delivery status on backend" })
        queries.updateOrderDeliveryStatus(
            status = status.name,
            updatedAt = kotlinx.datetime.Clock.System.now().toString(),
            orderId = orderId
        )
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> = runCatching {
        val now = kotlinx.datetime.Clock.System.now().toString()
        queries.updateOrderPaymentStatus(
            // PaymentStatus has no CANCELLED value. FAILED keeps the record
            // readable while deliveryStatus carries the cancellation state.
            status = "FAILED",
            txCode = null,
            updatedAt = now,
            orderId = orderId
        )
        queries.updateOrderDeliveryStatus(
            status = "CANCELLED",
            updatedAt = now,
            orderId = orderId
        )

        val response: ApiResponse<OrderDto> = client.post("$BASE_URL/orders/$orderId/cancel").body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to cancel order on server" })
        }
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> =
        queries.selectOrdersByCustomer(customerId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomainWithItems() } }

    override fun getOrdersByDateRange(
        businessId: String,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<Order>> {
        val startStr = start.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
        val endStr = end.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
        return queries.selectAllOrders(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.filter { it.created_at >= startStr && it.created_at <= endStr }
                    .map { it.toDomainWithItems() }
            }
    }

     override suspend fun generateOrderNumber(businessId: String): String {
         val count = queries.countOrdersForBusiness(businessId).executeAsOne()
         return "B360-${(count + 1).toString().padStart(4, '0')}"
     }

    /** Sync orders from API and update local cache **/
    suspend fun syncOrdersFromApi(businessId: String): Result<List<Order>> = runCatching {
        val response: ApiResponse<PagedOrdersDto> = client.get("$BASE_URL/orders") {
            url { parameters.append("pageSize", "100") }
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch orders" })
        }
        val remoteOrders = requireNotNull(response.data).data

        // Update local cache
        remoteOrders.forEach { dto ->
            queries.insertOrder(
                id = dto.id,
                order_number = dto.orderNumber,
                business_id = dto.businessId,
                customer_id = dto.customerId,
                customer_name = dto.customerName,
                customer_phone = dto.customerPhone,
                delivery_location = dto.deliveryLocation,
                payment_status = dto.paymentStatus,
                delivery_status = dto.deliveryStatus,
                payment_method = dto.paymentMethod,
                mpesa_transaction_code = dto.mpesaTransactionCode,
                notes = dto.notes,
                subtotal = dto.items.sumOf { it.quantity * it.unitPrice },
                created_at = dto.createdAt,
                updated_at = dto.updatedAt
            )

            dto.items.forEach { item ->
                queries.insertOrderItem(
                    id = generateId(),
                    order_id = dto.id,
                    product_id = item.productId,
                    product_name = item.productName,
                    quantity = item.quantity.toLong(),
                    unit_price = item.unitPrice,
                    buying_price = item.buyingPrice
                )
            }
        }

        remoteOrders.map { it.toDomain() }
    }

    private fun OrderDto.toDomain() = Order(
        id = id,
        orderNumber = orderNumber,
        businessId = businessId,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        deliveryLocation = deliveryLocation,
        items = items.map { OrderItem(
            productId = it.productId,
            productName = it.productName,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            buyingPrice = it.buyingPrice
        ) },
        paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.PENDING),
        deliveryStatus = runCatching { DeliveryStatus.valueOf(deliveryStatus) }.getOrDefault(DeliveryStatus.PENDING),
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.MPESA),
        mpesaTransactionCode = mpesaTransactionCode,
        notes = notes,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )

     private fun OrderEntity.toDomainWithItems(): Order {
         val items = queries.selectItemsByOrder(id).executeAsList().map { item ->
             OrderItem(
                 productId = item.product_id,
                 productName = item.product_name,
                 quantity = item.quantity.toInt(),
                 unitPrice = item.unit_price,
                 buyingPrice = item.buying_price
             )
         }
         return Order(
             id = id,
             orderNumber = order_number,
             businessId = business_id,
             customerId = customer_id,
             customerName = customer_name,
             customerPhone = customer_phone,
             deliveryLocation = delivery_location,
             items = items,
             paymentStatus = PaymentStatus.valueOf(payment_status),
             deliveryStatus = DeliveryStatus.valueOf(delivery_status),
             paymentMethod = runCatching { PaymentMethod.valueOf(payment_method) }
                 .getOrDefault(PaymentMethod.MPESA),
             mpesaTransactionCode = mpesa_transaction_code,
             notes = notes,
             createdAt = runCatching { Instant.parse(created_at) }
                 .getOrDefault(kotlinx.datetime.Clock.System.now()),
             updatedAt = runCatching { Instant.parse(updated_at) }
                 .getOrDefault(kotlinx.datetime.Clock.System.now())
         )
     }
}
