package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.ExpenseEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

class ExpenseRepositoryImpl(
    private val database: Biashara360Database
) : ExpenseRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getExpenses(businessId: String): Flow<List<Expense>> =
        queries.selectAllExpenses(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getExpensesByCategory(
        businessId: String,
        category: ExpenseCategory
    ): Flow<List<Expense>> =
        queries.selectExpensesByCategory(businessId, category.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getExpensesByDateRange(
        businessId: String,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<Expense>> =
        queries.selectExpensesByDateRange(businessId, start.toString(), end.toString())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override suspend fun saveExpense(expense: Expense): Result<Expense> = runCatching {
        queries.insertExpense(
            id = expense.id,
            business_id = expense.businessId,
            category = expense.category.name,
            amount = expense.amount,
            description = expense.description,
            receipt_url = expense.receiptUrl,
            recorded_at = expense.recordedAt.toString(),
            expense_date = expense.expenseDate.toString()
        )
        expense
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = runCatching {
        queries.deleteExpense(id)
    }

    override suspend fun getProfitSummary(
        businessId: String,
        period: ReportPeriod
    ): ProfitSummary {
        val startStr = period.startDate.toString()
        val endStr = period.endDate.toString()

        // Revenue: sum of order subtotals in the period (orders use ISO datetime, compare date prefix)
        val startInstant = period.startDate.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
        val endInstant = period.endDate.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
        val totalRevenue = queries.sumOrderRevenueByPeriod(businessId, startInstant, endInstant)
            .executeAsOne().COALESCE ?: 0.0

        // Cost of goods: sum from order items in the period
        val itemsInPeriod = queries.selectItemsByOrders(businessId, startInstant, endInstant)
            .executeAsList()
        val totalCostOfGoods = itemsInPeriod.sumOf { it.quantity * it.buying_price }

        val grossProfit = totalRevenue - totalCostOfGoods

        // Expenses
        val totalExpenses = queries.sumExpensesByPeriodAll(businessId, startStr, endStr)
            .executeAsOne().COALESCE ?: 0.0

        val netProfit = grossProfit - totalExpenses

        // Cashflow: payments received
        val cashflowIn = queries.sumPaymentsByPeriod(businessId, startInstant, endInstant)
            .executeAsOne().SUM ?: 0.0
        val cashflowOut = totalExpenses + totalCostOfGoods

        return ProfitSummary(
            businessId = businessId,
            period = period,
            totalRevenue = totalRevenue,
            totalCostOfGoods = totalCostOfGoods,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            cashflowIn = cashflowIn,
            cashflowOut = cashflowOut
        )
    }

    private fun ExpenseEntity.toDomain() = Expense(
        id = id,
        businessId = business_id,
        category = runCatching { ExpenseCategory.valueOf(category) }
            .getOrDefault(ExpenseCategory.MISCELLANEOUS),
        amount = amount,
        description = description,
        receiptUrl = receipt_url,
        recordedAt = runCatching { Instant.parse(recorded_at) }
            .getOrDefault(Clock.System.now()),
        expenseDate = runCatching { LocalDate.parse(expense_date) }
            .getOrDefault(Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date)
    )
}
