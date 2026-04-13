package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.OrderEntity
import com.app.biashara.db.OrderItemEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.OrderRepository
import com.app.biashara.domain.usecase.generateId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class OrderRepositoryImpl(
    private val database: Biashara360Database
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
        val now = kotlinx.datetime.Clock.System.now().toString()
        queries.insertOrder(
            id = order.id,
            order_number = order.orderNumber,
            business_id = order.businessId,
            customer_id = order.customerId,
            customer_name = order.customerName,
            customer_phone = order.customerPhone,
            delivery_location = order.deliveryLocation,
            payment_status = order.paymentStatus.name,
            delivery_status = order.deliveryStatus.name,
            payment_method = order.paymentMethod.name,
            mpesa_transaction_code = order.mpesaTransactionCode,
            notes = order.notes,
            subtotal = order.subtotal,
            created_at = now,
            updated_at = now
        )
        order.items.forEach { item ->
            queries.insertOrderItem(
                id = generateId(),
                order_id = order.id,
                product_id = item.productId,
                product_name = item.productName,
                quantity = item.quantity.toLong(),
                unit_price = item.unitPrice,
                buying_price = item.buyingPrice
            )
        }
        order
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
        queries.updateOrderDeliveryStatus(
            status = status.name,
            updatedAt = kotlinx.datetime.Clock.System.now().toString(),
            orderId = orderId
        )
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
        return "B360-${String.format("%04d", count + 1)}"
    }

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
