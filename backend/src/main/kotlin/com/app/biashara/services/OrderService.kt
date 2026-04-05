package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock

class OrderService {

    fun getAll(businessId: String, paymentStatus: String? = null, page: Int = 1, pageSize: Int = 20): PagedResponse<OrderResponse> = transaction {
        var query = OrdersTable.select { OrdersTable.businessId eq businessId }
        if (!paymentStatus.isNullOrBlank()) {
            query = query.andWhere { OrdersTable.paymentStatus eq paymentStatus }
        }
        val total = query.count().toInt()
        val orders = query
            .orderBy(OrdersTable.createdAt, SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { it.toResponse() }
        PagedResponse(orders, total, page, pageSize, (page * pageSize) < total)
    }

    fun getById(id: String, businessId: String): OrderResponse? = transaction {
        OrdersTable.select {
            (OrdersTable.id eq id) and (OrdersTable.businessId eq businessId)
        }.firstOrNull()?.toResponse()
    }

    fun create(businessId: String, req: CreateOrderRequest): ApiResponse<OrderResponse> = transaction {
        // Validate stock for all items
        for (item in req.items) {
            val product = ProductsTable.select {
                (ProductsTable.id eq item.productId) and (ProductsTable.businessId eq businessId)
            }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Product ${item.productId} not found")

            if (product[ProductsTable.currentStock] < item.quantity) {
                return@transaction ApiResponse(
                    false,
                    message = "Insufficient stock for ${product[ProductsTable.name]}: only ${product[ProductsTable.currentStock]} available"
                )
            }
        }

        val orderId = generateId()
        val orderNumber = generateOrderNumber(businessId)
        val now = Clock.System.now()
        val subtotal = req.items.sumOf { it.quantity * it.unitPrice }

        OrdersTable.insert {
            it[id] = orderId
            it[OrdersTable.orderNumber] = orderNumber
            it[OrdersTable.businessId] = businessId
            it[customerId] = req.customerId
            it[customerName] = req.customerName
            it[customerPhone] = req.customerPhone
            it[deliveryLocation] = req.deliveryLocation
            it[paymentStatus] = "PENDING"
            it[deliveryStatus] = "PENDING"
            it[paymentMethod] = req.paymentMethod
            it[notes] = req.notes
            it[OrdersTable.subtotal] = subtotal
            it[createdAt] = now
            it[updatedAt] = now
        }

        // Insert items + deduct stock
        req.items.forEach { item ->
            val product = ProductsTable.select { ProductsTable.id eq item.productId }.first()
            OrderItemsTable.insert {
                it[id] = generateId()
                it[OrderItemsTable.orderId] = orderId
                it[productId] = item.productId
                it[productName] = product[ProductsTable.name]
                it[quantity] = item.quantity
                it[unitPrice] = item.unitPrice
                it[buyingPrice] = product[ProductsTable.buyingPrice]
            }
            // Deduct stock
            ProductsTable.update({ ProductsTable.id eq item.productId }) {
                it[currentStock] = product[ProductsTable.currentStock] - item.quantity
                it[updatedAt] = now
            }
            // Stock movement record
            StockMovementsTable.insert {
                it[StockMovementsTable.id] = generateId()
                it[StockMovementsTable.productId] = item.productId
                it[StockMovementsTable.businessId] = businessId
                it[StockMovementsTable.type] = "STOCK_OUT"
                it[StockMovementsTable.quantity] = item.quantity
                it[StockMovementsTable.note] = "Order $orderNumber"
                it[StockMovementsTable.orderId] = orderId
                it[StockMovementsTable.recordedAt] = now
            }
        }

        // Award loyalty points (1 point per 100 KES)
        req.customerId?.let { cid ->
            val points = (subtotal / 100).toInt()
            if (points > 0) {
                CustomersTable.update({ CustomersTable.id eq cid }) {
                    val currentPoints = CustomersTable.select { CustomersTable.id eq cid }.first()[CustomersTable.loyaltyPoints]
                    it[loyaltyPoints] = currentPoints + points
                    it[updatedAt] = now
                }
            }
        }

        val order = OrdersTable.select { OrdersTable.id eq orderId }.first().toResponse()
        ApiResponse(true, data = order, message = "Order $orderNumber created")
    }

    fun updatePaymentStatus(id: String, businessId: String, req: UpdatePaymentStatusRequest): ApiResponse<OrderResponse> = transaction {
        val updated = OrdersTable.update({
            (OrdersTable.id eq id) and (OrdersTable.businessId eq businessId)
        }) {
            it[paymentStatus] = req.status
            if (req.mpesaTransactionCode != null) it[mpesaTransactionCode] = req.mpesaTransactionCode
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Order not found")
        val order = OrdersTable.select { OrdersTable.id eq id }.first().toResponse()
        ApiResponse(true, data = order)
    }

    fun updateDeliveryStatus(id: String, businessId: String, req: UpdateDeliveryStatusRequest): ApiResponse<OrderResponse> = transaction {
        val updated = OrdersTable.update({
            (OrdersTable.id eq id) and (OrdersTable.businessId eq businessId)
        }) {
            it[deliveryStatus] = req.status
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Order not found")
        val order = OrdersTable.select { OrdersTable.id eq id }.first().toResponse()
        ApiResponse(true, data = order)
    }

    private fun generateOrderNumber(businessId: String): String {
        val count = OrdersTable.select { OrdersTable.businessId eq businessId }.count()
        return "B360-%04d".format(count + 1)
    }

    private fun ResultRow.toResponse(): OrderResponse {
        val orderId = this[OrdersTable.id]
        val items = OrderItemsTable.select { OrderItemsTable.orderId eq orderId }.map { item ->
            val qty = item[OrderItemsTable.quantity]
            val price = item[OrderItemsTable.unitPrice]
            val buying = item[OrderItemsTable.buyingPrice]
            OrderItemResponse(
                id = item[OrderItemsTable.id],
                productId = item[OrderItemsTable.productId],
                productName = item[OrderItemsTable.productName],
                quantity = qty,
                unitPrice = price,
                buyingPrice = buying,
                lineTotal = qty * price,
                lineProfit = qty * (price - buying)
            )
        }
        return OrderResponse(
            id = orderId,
            orderNumber = this[OrdersTable.orderNumber],
            businessId = this[OrdersTable.businessId],
            customerId = this[OrdersTable.customerId],
            customerName = this[OrdersTable.customerName],
            customerPhone = this[OrdersTable.customerPhone],
            deliveryLocation = this[OrdersTable.deliveryLocation],
            items = items,
            paymentStatus = this[OrdersTable.paymentStatus],
            deliveryStatus = this[OrdersTable.deliveryStatus],
            paymentMethod = this[OrdersTable.paymentMethod],
            mpesaTransactionCode = this[OrdersTable.mpesaTransactionCode],
            subtotal = this[OrdersTable.subtotal],
            notes = this[OrdersTable.notes],
            createdAt = this[OrdersTable.createdAt].toString(),
            updatedAt = this[OrdersTable.updatedAt].toString()
        )
    }
}
