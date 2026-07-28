package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock

internal data class InitialOrderStatuses(
    val payment: String,
    val delivery: String
)

internal fun resolveInitialOrderStatuses(
    paymentMethod: String,
    requestedPaymentStatus: String?,
    requestedDeliveryStatus: String?,
    deliveryLocation: String
): InitialOrderStatuses {
    val method = paymentMethod.trim().uppercase()
    val requestedPayment = requestedPaymentStatus?.trim()?.uppercase()
    val requestedDelivery = requestedDeliveryStatus?.trim()?.uppercase()
    val allowedPaymentStatuses = setOf("PAID", "PENDING", "COD", "FAILED")
    val allowedDeliveryStatuses = setOf("PENDING", "PROCESSING", "SHIPPED", "DELIVERED")

    val payment = when {
        requestedPayment in setOf("PAID", "COMPLETED") -> "PAID"
        method == "CASH" -> "PAID"
        method == "COD" -> "COD"
        requestedPayment in allowedPaymentStatuses -> requestedPayment!!
        else -> "PENDING"
    }
    val isInStoreSale = deliveryLocation.trim().isEmpty() ||
        deliveryLocation.trim().lowercase() in setOf("pos", "in-store pos", "in store pos")
    val delivery = when {
        requestedDelivery in allowedDeliveryStatuses -> requestedDelivery!!
        method == "CASH" && isInStoreSale -> "DELIVERED"
        else -> "PENDING"
    }
    return InitialOrderStatuses(payment, delivery)
}

class OrderService {

    fun getAll(businessId: String, paymentStatus: String? = null, page: Int = 1, pageSize: Int = 20): PagedResponse<OrderResponse> = transaction {
        var query = OrdersTable.select { OrdersTable.businessId eq businessId }
        if (!paymentStatus.isNullOrBlank()) {
            query = query.andWhere { OrdersTable.paymentStatus eq paymentStatus }
        }
        val total = query.count().toInt()
        val orderRows = query
            .orderBy(OrdersTable.createdAt, SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .toList()

        // Batch-fetch items for all orders in one query (avoids N+1)
        val orderIds = orderRows.map { it[OrdersTable.id] }
        val itemsByOrderId: Map<String, List<OrderItemResponse>> = if (orderIds.isEmpty()) emptyMap() else {
            OrderItemsTable
                .select { OrderItemsTable.orderId inList orderIds }
                .groupBy { it[OrderItemsTable.orderId] }
                .mapValues { (_, rows) ->
                    rows.map { item ->
                        val qty   = item[OrderItemsTable.quantity]
                        val price = item[OrderItemsTable.unitPrice]
                        val buy   = item[OrderItemsTable.buyingPrice]
                        OrderItemResponse(
                            id          = item[OrderItemsTable.id],
                            productId   = item[OrderItemsTable.productId],
                            productName = item[OrderItemsTable.productName],
                            quantity    = qty,
                            unitPrice   = price,
                            buyingPrice = buy,
                            lineTotal   = qty * price,
                            lineProfit  = qty * (price - buy)
                        )
                    }
                }
        }

        val orders = orderRows.map { it.toResponse(itemsByOrderId[it[OrdersTable.id]] ?: emptyList()) }
        PagedResponse(orders, total, page, pageSize, (page * pageSize) < total)
    }

    fun getById(id: String, businessId: String): OrderResponse? = transaction {
        OrdersTable.select {
            (OrdersTable.id eq id) and (OrdersTable.businessId eq businessId)
        }.firstOrNull()?.toResponse()
    }

    fun create(
        businessId: String,
        req: CreateOrderRequest,
        clientPlatform: String? = null
    ): ApiResponse<OrderResponse> = transaction {
        val clientReference = req.clientReference?.trim()?.takeIf { it.isNotEmpty() }
        if (clientReference != null &&
            (clientReference.length > 64 || !clientReference.matches(Regex("^[A-Za-z0-9._:-]+$")))
        ) {
            return@transaction ApiResponse(
                false,
                message = "Invalid client transaction reference"
            )
        }
        if (clientReference != null) {
            val existing = OrdersTable.select {
                (OrdersTable.businessId eq businessId) and
                    (OrdersTable.clientReference eq clientReference)
            }.firstOrNull()
            if (existing != null) {
                val existingOrder = existing.toResponse()
                val sameTransaction =
                    existingOrder.customerId == req.customerId &&
                    existingOrder.customerName == req.customerName &&
                    existingOrder.customerPhone == req.customerPhone &&
                    existingOrder.paymentMethod == req.paymentMethod &&
                    existingOrder.items.size == req.items.size &&
                    existingOrder.items.zip(req.items).all { (saved, submitted) ->
                        saved.productId == submitted.productId &&
                            saved.quantity == submitted.quantity &&
                            saved.unitPrice == submitted.unitPrice
                    }
                if (!sameTransaction) {
                    return@transaction ApiResponse(
                        false,
                        message = "Client transaction reference is already used by another order"
                    )
                }
                return@transaction ApiResponse(
                    true,
                    data = existingOrder,
                    message = "Existing order returned for repeated transaction"
                )
            }
        }

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
        val orderNumber = generateOrderNumber(clientPlatform)
        val now = Clock.System.now()
        val subtotal = req.items.sumOf { it.quantity * it.unitPrice }

        val initialStatuses = resolveInitialOrderStatuses(
            req.paymentMethod,
            req.paymentStatus,
            req.deliveryStatus,
            req.deliveryLocation
        )

        OrdersTable.insert {
            it[id] = orderId
            it[OrdersTable.orderNumber] = orderNumber
            it[OrdersTable.businessId] = businessId
            it[OrdersTable.clientReference] = clientReference
            it[customerId] = req.customerId
            it[customerName] = req.customerName
            it[customerPhone] = req.customerPhone
            it[deliveryLocation] = req.deliveryLocation
            it[paymentStatus] = initialStatuses.payment
            it[deliveryStatus] = initialStatuses.delivery
            it[paymentMethod] = req.paymentMethod
            it[notes] = req.notes
            it[OrdersTable.subtotal] = subtotal
            it[createdAt] = now
            it[updatedAt] = now
        }

        // Insert items + deduct stock atomically (conditional UPDATE prevents TOCTOU race)
        for (item in req.items) {
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
            // Atomic conditional deduction: only succeeds if stock is still sufficient
            val deducted = ProductsTable.update({
                (ProductsTable.id eq item.productId) and
                (ProductsTable.currentStock greaterEq item.quantity)
            }) {
                with(SqlExpressionBuilder) { it.update(currentStock, currentStock - item.quantity) }
                it[updatedAt] = now
            }
            if (deducted == 0) {
                // Stock was concurrently depleted — roll back the transaction
                return@transaction ApiResponse(
                    false,
                    message = "Insufficient stock for ${product[ProductsTable.name]}: concurrent order may have consumed remaining units"
                )
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


        // Award loyalty points (1 point per 100 KES) — SQL increment avoids lost-update race
        req.customerId?.let { cid ->
            val points = (subtotal / 100).toInt()
            if (points > 0) {
                CustomersTable.update({ CustomersTable.id eq cid }) {
                    with(SqlExpressionBuilder) {
                        it.update(loyaltyPoints, loyaltyPoints + points)
                    }
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

    fun cancel(id: String, businessId: String): ApiResponse<OrderResponse> =
        closeOrder(id, businessId, isVoid = false)

    fun void(id: String, businessId: String): ApiResponse<OrderResponse> =
        closeOrder(id, businessId, isVoid = true)

    private fun closeOrder(id: String, businessId: String, isVoid: Boolean): ApiResponse<OrderResponse> = transaction {
        val order = OrdersTable.select {
            (OrdersTable.id eq id) and (OrdersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Order not found")

        val currentPaymentStatus = order[OrdersTable.paymentStatus]
        val currentDeliveryStatus = order[OrdersTable.deliveryStatus]
        if (currentPaymentStatus in setOf("CANCELLED", "REFUNDED") || currentDeliveryStatus == "CANCELLED") {
            return@transaction ApiResponse(false, message = "Order is already cancelled or voided")
        }
        if (!isVoid && currentPaymentStatus == "PAID") {
            return@transaction ApiResponse(false, message = "Cannot cancel a paid order. Please initiate a refund instead.")
        }

        // Revert stock for all items
        val items = OrderItemsTable.select { OrderItemsTable.orderId eq id }
        val now = Clock.System.now()
        items.forEach { item ->
            val productId = item[OrderItemsTable.productId]
            val quantity = item[OrderItemsTable.quantity]
            
            val product = ProductsTable.select { ProductsTable.id eq productId }.firstOrNull()
            if (product != null) {
                // SQL increment to avoid read-modify-write race on stock
                ProductsTable.update({ ProductsTable.id eq productId }) {
                    with(SqlExpressionBuilder) { it.update(currentStock, currentStock + quantity) }
                    it[updatedAt] = now
                }
                
                // Add stock movement record (STOCK_IN/CANCEL_ORDER)
                StockMovementsTable.insert {
                    it[StockMovementsTable.id] = generateId()
                    it[StockMovementsTable.productId] = productId
                    it[StockMovementsTable.businessId] = businessId
                    it[StockMovementsTable.type] = "STOCK_IN"
                    it[StockMovementsTable.quantity] = quantity
                    it[StockMovementsTable.note] =
                        "${if (isVoid) "Voided" else "Cancelled"} Order ${order[OrdersTable.orderNumber]}"
                    it[StockMovementsTable.orderId] = id
                    it[StockMovementsTable.recordedAt] = now
                }
            }
        }

        // Revert loyalty points — SQL expression prevents lost-update under concurrency
        order[OrdersTable.customerId]?.let { cid ->
            val subtotal = order[OrdersTable.subtotal]
            val points = (subtotal / 100).toInt()
            if (points > 0) {
                CustomersTable.update({ CustomersTable.id eq cid }) {
                    // GREATEST(0, current - points) in SQL via Exposed
                    with(SqlExpressionBuilder) {
                        it.update(loyaltyPoints, case()
                            .When(loyaltyPoints greater points, loyaltyPoints - points)
                            .Else(intLiteral(0)))
                    }
                    it[updatedAt] = now
                }
            }
        }

        // Update order status
        OrdersTable.update({ OrdersTable.id eq id }) {
            it[paymentStatus] = if (isVoid) "REFUNDED" else "CANCELLED"
            it[deliveryStatus] = "CANCELLED"
            it[updatedAt] = now
        }

        val updatedOrder = OrdersTable.select { OrdersTable.id eq id }.first().toResponse()
        val action = if (isVoid) "voided" else "cancelled"
        ApiResponse(true, data = updatedOrder, message = "Order ${order[OrdersTable.orderNumber]} $action successfully")
    }

    private fun generateOrderNumber(clientPlatform: String?): String {
        val platform = when (clientPlatform?.trim()?.lowercase()) {
            "desktop" -> "DESK"
            "android" -> "ANDR"
            "ios" -> "IOS"
            "web" -> "WEB"
            "social" -> "SOC"
            else -> "WEB"
        }

        // The unique database index is the final guard. Checking candidates
        // here gives every supported client a readable, globally unique
        // reference while remaining within the existing VARCHAR(20) column.
        repeat(10) {
            val suffix = java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .take(8)
                .uppercase()
            val candidate = "B360-$platform-$suffix"
            val exists = OrdersTable
                .select { OrdersTable.orderNumber eq candidate }
                .limit(1)
                .any()
            if (!exists) return candidate
        }
        throw IllegalStateException("Unable to generate a unique order reference")
    }

    private fun ResultRow.toResponse(preloadedItems: List<OrderItemResponse>? = null): OrderResponse {
        val orderId = this[OrdersTable.id]
        val items = preloadedItems ?: OrderItemsTable.select { OrderItemsTable.orderId eq orderId }.map { item ->
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
