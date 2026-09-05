package com.app.biashara.services

import com.app.biashara.db.OrdersTable
import com.app.biashara.db.OrderItemsTable
import com.app.biashara.db.UsersTable
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class PortalOrderItem(val name: String, val quantity: Int)

@Serializable
data class PortalOrderSummary(
    val id: String, val orderNumber: String, val customerName: String,
    val location: String, val amount: Double, val paymentStatus: String,
    val createdAt: String, val claimedBy: String?, val items: List<PortalOrderItem>
)

@Serializable
data class PortalOrderQueue(val waiting: List<PortalOrderSummary>, val mine: List<PortalOrderSummary>)

class PortalOrderService {
    private val fields = listOf(OrdersTable.id, OrdersTable.orderNumber, OrdersTable.customerName,
        OrdersTable.deliveryLocation, OrdersTable.subtotal, OrdersTable.paymentStatus,
        OrdersTable.createdAt, OrdersTable.serverUserId)

    private fun eligible(businessId: String): Op<Boolean> = Op.build {
        (OrdersTable.businessId eq businessId) and (OrdersTable.salesChannel eq "ECOMMERCE") and
            (OrdersTable.paymentStatus notInList listOf("CANCELLED", "VOIDED", "REFUNDED")) and
            (OrdersTable.deliveryStatus notInList listOf("CANCELLED", "DELIVERED", "RETURNED"))
    }

    private fun requireMember(businessId: String, userId: String) {
        require(UsersTable.slice(UsersTable.id).select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true)
        }.any()) { "Active business membership is required" }
    }

    fun queue(businessId: String, userId: String): PortalOrderQueue = transaction {
        requireMember(businessId, userId)
        val waiting = OrdersTable.slice(fields).select {
            eligible(businessId) and OrdersTable.serverUserId.isNull()
        }.orderBy(OrdersTable.createdAt, SortOrder.ASC).limit(100).toList()
        val mine = OrdersTable.slice(fields).select {
            eligible(businessId) and (OrdersTable.serverUserId eq userId)
        }.orderBy(OrdersTable.createdAt, SortOrder.DESC).limit(100).toList()
        val items = itemsFor((waiting + mine).map { it[OrdersTable.id] })
        PortalOrderQueue(waiting.map { summary(it, items) }, mine.map { summary(it, items) })
    }

    // The unclaimed predicate is part of the UPDATE, so competing claims cannot overwrite the winner.
    fun claim(businessId: String, userId: String, orderId: String): PortalOrderSummary? = transaction {
        requireMember(businessId, userId)
        val updated = OrdersTable.update({
            eligible(businessId) and (OrdersTable.id eq orderId) and OrdersTable.serverUserId.isNull()
        }) {
            it[serverUserId] = userId
            it[updatedAt] = Clock.System.now()
        }
        // Retrying a successful claim by the same user is safe.
        val row = OrdersTable.slice(fields).select {
            eligible(businessId) and (OrdersTable.id eq orderId) and (OrdersTable.serverUserId eq userId)
        }.firstOrNull() ?: return@transaction null
        check(updated in 0..1)
        summary(row, itemsFor(listOf(orderId)))
    }

    private fun itemsFor(ids: List<String>): Map<String, List<PortalOrderItem>> {
        if (ids.isEmpty()) return emptyMap()
        return OrderItemsTable.slice(OrderItemsTable.orderId, OrderItemsTable.productName, OrderItemsTable.quantity)
            .select { OrderItemsTable.orderId inList ids }.groupBy { it[OrderItemsTable.orderId] }
            .mapValues { (_, rows) -> rows.map { PortalOrderItem(it[OrderItemsTable.productName], it[OrderItemsTable.quantity]) } }
    }

    private fun summary(row: ResultRow, items: Map<String, List<PortalOrderItem>>) = PortalOrderSummary(
        row[OrdersTable.id], row[OrdersTable.orderNumber], row[OrdersTable.customerName],
        row[OrdersTable.deliveryLocation], row[OrdersTable.subtotal], row[OrdersTable.paymentStatus],
        row[OrdersTable.createdAt].toString(), row[OrdersTable.serverUserId], items[row[OrdersTable.id]].orEmpty()
    )
}
