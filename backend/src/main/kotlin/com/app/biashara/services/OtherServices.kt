package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

// ─── Customer Service ─────────────────────────────────────────────────────────

class CustomerService {

    fun getAll(businessId: String, query: String? = null): List<CustomerResponse> = transaction {
        var stmt = CustomersTable.select {
            (CustomersTable.businessId eq businessId) and (CustomersTable.isActive eq true)
        }
        if (!query.isNullOrBlank()) {
            stmt = stmt.andWhere {
                (CustomersTable.name.lowerCase() like "%${query.lowercase()}%") or
                (CustomersTable.phone like "%$query%")
            }
        }
        stmt.orderBy(CustomersTable.name).map { it.toResponse(businessId) }
    }

    fun getById(id: String, businessId: String): CustomerResponse? = transaction {
        CustomersTable.select {
            (CustomersTable.id eq id) and (CustomersTable.businessId eq businessId)
        }.firstOrNull()?.toResponse(businessId)
    }

    fun create(businessId: String, req: CustomerRequest): ApiResponse<CustomerResponse> = transaction {
        val existing = CustomersTable.select {
            (CustomersTable.businessId eq businessId) and (CustomersTable.phone eq req.phone)
        }.firstOrNull()
        if (existing != null) return@transaction ApiResponse(false, message = "Customer with this phone already exists")

        val id = generateId()
        val now = Clock.System.now()
        CustomersTable.insert {
            it[CustomersTable.id] = id
            it[CustomersTable.businessId] = businessId
            it[name] = req.name
            it[phone] = req.phone
            it[email] = req.email
            it[location] = req.location
            it[notes] = req.notes
            it[createdAt] = now
            it[updatedAt] = now
        }
        val customer = CustomersTable.select { CustomersTable.id eq id }.first().toResponse(businessId)
        ApiResponse(true, data = customer, message = "Customer added")
    }

    fun update(id: String, businessId: String, req: CustomerRequest): ApiResponse<CustomerResponse> = transaction {
        val updated = CustomersTable.update({
            (CustomersTable.id eq id) and (CustomersTable.businessId eq businessId)
        }) {
            it[name] = req.name
            it[phone] = req.phone
            it[email] = req.email
            it[location] = req.location
            it[notes] = req.notes
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Customer not found")
        val customer = CustomersTable.select { CustomersTable.id eq id }.first().toResponse(businessId)
        ApiResponse(true, data = customer)
    }

    fun getTopCustomers(businessId: String, limit: Int = 10): List<CustomerResponse> = transaction {
        // Join with orders to rank by total spent
        val topIds = (OrdersTable innerJoin CustomersTable)
            .slice(OrdersTable.customerId, OrdersTable.subtotal.sum())
            .select { (OrdersTable.businessId eq businessId) and (OrdersTable.customerId.isNotNull()) }
            .groupBy(OrdersTable.customerId)
            .orderBy(OrdersTable.subtotal.sum(), SortOrder.DESC)
            .limit(limit)
            .mapNotNull { it[OrdersTable.customerId] }

        topIds.mapNotNull { cid ->
            CustomersTable.select { CustomersTable.id eq cid }.firstOrNull()?.toResponse(businessId)
        }
    }

    private fun ResultRow.toResponse(businessId: String): CustomerResponse {
        val customerId = this[CustomersTable.id]
        val orderStats = OrdersTable
            .slice(OrdersTable.id.count(), OrdersTable.subtotal.sum())
            .select { (OrdersTable.customerId eq customerId) and (OrdersTable.paymentStatus eq "PAID") }
            .first()
        val totalOrders = orderStats[OrdersTable.id.count()].toInt()
        val totalSpent = orderStats[OrdersTable.subtotal.sum()] ?: 0.0

        return CustomerResponse(
            id = customerId,
            businessId = this[CustomersTable.businessId],
            name = this[CustomersTable.name],
            phone = this[CustomersTable.phone],
            email = this[CustomersTable.email],
            location = this[CustomersTable.location],
            notes = this[CustomersTable.notes],
            loyaltyPoints = this[CustomersTable.loyaltyPoints],
            totalOrders = totalOrders,
            totalSpent = totalSpent,
            isRepeatCustomer = totalOrders > 1,
            createdAt = this[CustomersTable.createdAt].toString()
        )
    }
}

// ─── Expense Service ──────────────────────────────────────────────────────────

class ExpenseService {

    fun getAll(businessId: String, category: String? = null, startDate: String? = null, endDate: String? = null): List<ExpenseResponse> = transaction {
        var stmt = ExpensesTable.select { ExpensesTable.businessId eq businessId }
        if (!category.isNullOrBlank()) stmt = stmt.andWhere { ExpensesTable.category eq category }
        if (!startDate.isNullOrBlank()) {
            val start = kotlinx.datetime.LocalDate.parse(startDate)
            stmt = stmt.andWhere { ExpensesTable.expenseDate greaterEq start }
        }
        if (!endDate.isNullOrBlank()) {
            val end = kotlinx.datetime.LocalDate.parse(endDate)
            stmt = stmt.andWhere { ExpensesTable.expenseDate lessEq end }
        }
        stmt.orderBy(ExpensesTable.expenseDate, SortOrder.DESC).map { it.toResponse() }
    }

    fun create(businessId: String, req: ExpenseRequest): ApiResponse<ExpenseResponse> = transaction {
        val id = generateId()
        val now = Clock.System.now()
        ExpensesTable.insert {
            it[ExpensesTable.id] = id
            it[ExpensesTable.businessId] = businessId
            it[category] = req.category
            it[amount] = req.amount
            it[description] = req.description
            it[expenseDate] = kotlinx.datetime.LocalDate.parse(req.expenseDate)
            it[receiptUrl] = req.receiptUrl
            it[recordedAt] = now
        }
        val expense = ExpensesTable.select { ExpensesTable.id eq id }.first().toResponse()
        ApiResponse(true, data = expense, message = "Expense recorded")
    }

    fun delete(id: String, businessId: String): ApiResponse<Unit> = transaction {
        val deleted = ExpensesTable.deleteWhere {
            (ExpensesTable.id eq id) and (ExpensesTable.businessId eq businessId)
        }
        if (deleted == 0) ApiResponse(false, message = "Expense not found")
        else ApiResponse(true, message = "Expense deleted")
    }

    fun getProfitSummary(businessId: String, startDate: String, endDate: String): ProfitSummaryResponse = transaction {
        val start = kotlinx.datetime.LocalDate.parse(startDate)
        val end   = kotlinx.datetime.LocalDate.parse(endDate)

        // Convert date range to instants for the orders timestamp column
        val tz         = TimeZone.of("Africa/Nairobi")
        val startInstant = start.atStartOfDayIn(tz)
        val endInstant   = end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

        // Revenue: paid orders in the date range
        val totalRevenue = OrdersTable
            .slice(OrdersTable.subtotal.sum())
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus eq "PAID") and
                (OrdersTable.createdAt greaterEq startInstant) and
                (OrdersTable.createdAt less endInstant)
            }.first()[OrdersTable.subtotal.sum()] ?: 0.0

        // COGS: sum of (buyingPrice × quantity) for items in paid orders in the date range
        val paidOrderIds = OrdersTable
            .slice(OrdersTable.id)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus eq "PAID") and
                (OrdersTable.createdAt greaterEq startInstant) and
                (OrdersTable.createdAt less endInstant)
            }.map { it[OrdersTable.id] }

        val totalCOGS = if (paidOrderIds.isEmpty()) 0.0
        else OrderItemsTable
            .slice(OrderItemsTable.buyingPrice, OrderItemsTable.quantity)
            .select { OrderItemsTable.orderId inList paidOrderIds }
            .sumOf { it[OrderItemsTable.buyingPrice] * it[OrderItemsTable.quantity] }

        val totalExpenses = ExpensesTable
            .slice(ExpensesTable.amount.sum())
            .select {
                (ExpensesTable.businessId eq businessId) and
                (ExpensesTable.expenseDate greaterEq start) and
                (ExpensesTable.expenseDate lessEq end)
            }.first()[ExpensesTable.amount.sum()] ?: 0.0

        val grossProfit = totalRevenue - totalCOGS
        val netProfit = grossProfit - totalExpenses

        ProfitSummaryResponse(
            period = "$startDate to $endDate",
            totalRevenue = totalRevenue,
            totalCostOfGoods = totalCOGS,
            grossProfit = grossProfit,
            grossMargin = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0,
            cashflowIn = totalRevenue,
            cashflowOut = totalCOGS + totalExpenses
        )
    }

    private fun ResultRow.toResponse() = ExpenseResponse(
        id = this[ExpensesTable.id],
        businessId = this[ExpensesTable.businessId],
        category = this[ExpensesTable.category],
        amount = this[ExpensesTable.amount],
        description = this[ExpensesTable.description],
        expenseDate = this[ExpensesTable.expenseDate].toString(),
        receiptUrl = this[ExpensesTable.receiptUrl],
        recordedAt = this[ExpensesTable.recordedAt].toString()
    )
}

// ─── Payment Service ──────────────────────────────────────────────────────────

class PaymentService {

    fun getAll(businessId: String, reconciledOnly: Boolean? = null): List<PaymentResponse> = transaction {
        var stmt = PaymentsTable.select { PaymentsTable.businessId eq businessId }
        if (reconciledOnly != null) stmt = stmt.andWhere { PaymentsTable.reconciled eq reconciledOnly }
        stmt.orderBy(PaymentsTable.transactionDate, SortOrder.DESC).map { it.toResponse() }
    }

    fun save(businessId: String, payment: PaymentsTable.() -> Unit): String = transaction {
        val id = generateId()
        PaymentsTable.insert {
            it[PaymentsTable.id] = id
            payment(PaymentsTable)
        }
        id
    }

    fun reconcile(paymentId: String, businessId: String, req: ReconcileRequest): ApiResponse<Unit> = transaction {
        val updated = PaymentsTable.update({
            (PaymentsTable.id eq paymentId) and (PaymentsTable.businessId eq businessId)
        }) {
            it[reconciled] = true
            it[orderId] = req.orderId
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Payment not found")

        // Update order payment status
        OrdersTable.update({ OrdersTable.id eq req.orderId }) {
            it[paymentStatus] = "PAID"
            it[updatedAt] = Clock.System.now()
        }
        ApiResponse(true, message = "Payment reconciled")
    }

    private fun ResultRow.toResponse() = PaymentResponse(
        id = this[PaymentsTable.id],
        businessId = this[PaymentsTable.businessId],
        orderId = this[PaymentsTable.orderId],
        transactionCode = this[PaymentsTable.transactionCode],
        amount = this[PaymentsTable.amount],
        payerPhone = this[PaymentsTable.payerPhone],
        payerName = this[PaymentsTable.payerName],
        method = this[PaymentsTable.method],
        status = this[PaymentsTable.status],
        channel = this[PaymentsTable.channel],
        reconciled = this[PaymentsTable.reconciled],
        transactionDate = this[PaymentsTable.transactionDate].toString()
    )
}

@kotlinx.serialization.Serializable
data class PaymentResponse(
    val id: String, val businessId: String, val orderId: String?,
    val transactionCode: String, val amount: Double,
    val payerPhone: String, val payerName: String,
    val method: String, val status: String, val channel: String,
    val reconciled: Boolean, val transactionDate: String
)

// ─── Dashboard Service ────────────────────────────────────────────────────────

class DashboardService(
    private val productService: ProductService,
    private val orderService: OrderService
) {

    fun getDashboard(businessId: String): DashboardResponse = transaction {
        val tz         = TimeZone.of("Africa/Nairobi")
        val now        = Clock.System.now()
        val localNow   = now.toLocalDateTime(tz)
        val today      = localNow.date
        val todayStart = today.atStartOfDayIn(tz)
        val monthStart = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(tz)

        // Revenue this calendar month (paid orders)
        val totalRevenueMonth = OrdersTable
            .slice(OrdersTable.subtotal.sum())
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus eq "PAID") and
                (OrdersTable.createdAt greaterEq monthStart) and
                (OrdersTable.createdAt less now)  // up to now within the month
            }.first()[OrdersTable.subtotal.sum()] ?: 0.0

        // COGS for paid orders this month
        val monthPaidIds = OrdersTable
            .slice(OrdersTable.id)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus eq "PAID") and
                (OrdersTable.createdAt greaterEq monthStart) and
                (OrdersTable.createdAt less now)
            }.map { it[OrdersTable.id] }

        val totalCogsMonth = if (monthPaidIds.isEmpty()) 0.0
        else OrderItemsTable
            .slice(OrderItemsTable.buyingPrice, OrderItemsTable.quantity)
            .select { OrderItemsTable.orderId inList monthPaidIds }
            .sumOf { it[OrderItemsTable.buyingPrice] * it[OrderItemsTable.quantity] }

        // Expenses this month (by recordedAt timestamp)
        val totalExpensesMonth = ExpensesTable
            .slice(ExpensesTable.amount.sum())
            .select {
                (ExpensesTable.businessId eq businessId) and
                (ExpensesTable.recordedAt greaterEq monthStart) and
                (ExpensesTable.recordedAt less now)
            }.first()[ExpensesTable.amount.sum()] ?: 0.0

        val netProfitMonth = totalRevenueMonth - totalCogsMonth - totalExpensesMonth

        // Orders created today
        val totalOrdersToday = OrdersTable
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.createdAt greaterEq todayStart)
            }.count().toInt()

        // Orders with pending payment
        val pendingOrdersCount = OrdersTable
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus eq "PENDING")
            }.count().toInt()

        // Orders not yet paid
        val unpaidOrdersCount = OrdersTable
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.paymentStatus neq "PAID")
            }.count().toInt()

        // Products below or at low-stock threshold
        val lowStockCount = ProductsTable
            .select {
                (ProductsTable.businessId eq businessId) and
                (ProductsTable.isActive eq true) and
                (ProductsTable.currentStock lessEq ProductsTable.lowStockThreshold)
            }.count().toInt()

        // Active customers
        val totalCustomers = CustomersTable
            .select {
                (CustomersTable.businessId eq businessId) and
                (CustomersTable.isActive eq true)
            }.count().toInt()

        // Delegate to existing services for rich object lists (avoids duplicating row mappers)
        val recentOrders    = orderService.getAll(businessId, null, 1, 5).data
        val lowStockProducts = productService.getAll(businessId, null, lowStockOnly = true).take(10)

        DashboardResponse(
            totalRevenueMonth  = totalRevenueMonth,
            netProfitMonth     = netProfitMonth,
            totalOrdersToday   = totalOrdersToday,
            pendingOrdersCount = pendingOrdersCount,
            lowStockCount      = lowStockCount,
            totalCustomers     = totalCustomers,
            unpaidOrdersCount  = unpaidOrdersCount,
            recentOrders       = recentOrders,
            lowStockProducts   = lowStockProducts
        )
    }
}
