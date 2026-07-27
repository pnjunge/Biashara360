package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.ExpenseEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.ExpenseRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

@kotlinx.serialization.Serializable
data class ExpenseDto(
    val id: String,
    val businessId: String,
    val category: String,
    val amount: Double,
    val description: String,
    val receiptUrl: String? = null,
    val recordedAt: String,
    val expenseDate: String
)

@kotlinx.serialization.Serializable
private data class ExpenseRequestDto(
    val category: String,
    val amount: Double,
    val description: String,
    val expenseDate: String,
    val receiptUrl: String? = null
)

class ExpenseRepositoryImpl(
    private val database: Biashara360Database,
    private val client: HttpClient? = null
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
        val httpClient = client ?: throw IllegalStateException("HTTP client not configured")
        val response: ApiResponse<ExpenseDto> = httpClient.post("$BASE_URL/expenses") {
            contentType(ContentType.Application.Json)
            setBody(
                ExpenseRequestDto(
                    category = expense.category.name,
                    amount = expense.amount,
                    description = expense.description,
                    expenseDate = expense.expenseDate.toString(),
                    receiptUrl = expense.receiptUrl
                )
            )
        }.body()
        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to save expense on backend" })
        }
        val saved = response.data
        queries.insertExpense(
            id = saved.id,
            business_id = saved.businessId,
            category = saved.category,
            amount = saved.amount,
            description = saved.description,
            receipt_url = saved.receiptUrl,
            recorded_at = saved.recordedAt,
            expense_date = saved.expenseDate
        )
        saved.toDomain()
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = runCatching {
        val httpClient = client ?: throw IllegalStateException("HTTP client not configured")
        val response: ApiResponse<Unit> = httpClient.delete("$BASE_URL/expenses/$id").body()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Failed to delete expense on backend" })
        }
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
        val endInstant = period.endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
        val totalRevenue = queries.sumOrderRevenueByPeriod(businessId, startInstant, endInstant)
            .executeAsOne().total_revenue ?: 0.0

        // Cost of goods: sum from order items in the period
        val itemsInPeriod = queries.selectItemsByOrders(businessId, startInstant, endInstant)
            .executeAsList()
        val totalCostOfGoods = itemsInPeriod.sumOf { it.quantity * it.buying_price }

        val grossProfit = totalRevenue - totalCostOfGoods

        // Expenses
        val totalExpenses = queries.sumExpensesByPeriodAll(businessId, startStr, endStr)
            .executeAsOne().total_amount ?: 0.0

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

    /** Sync expenses from API and update local cache **/
    suspend fun syncExpensesFromApi(businessId: String): Result<List<Expense>> = runCatching {
        if (client == null) throw IllegalStateException("HTTP client not configured")

        val response: ApiResponse<List<ExpenseDto>> = client.get("$BASE_URL/expenses") {
            url { parameters.append("businessId", businessId) }
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch expenses" })
        }

        val remoteIds = response.data.map { it.id }.toSet()
        queries.selectAllExpenses(businessId).executeAsList()
            .filter { it.id !in remoteIds }
            .forEach { queries.deleteExpense(it.id) }

        // Update local cache
        response.data.forEach { dto ->
            queries.insertExpense(
                id = dto.id,
                business_id = dto.businessId,
                category = dto.category,
                amount = dto.amount,
                description = dto.description,
                receipt_url = dto.receiptUrl,
                recorded_at = dto.recordedAt,
                expense_date = dto.expenseDate
            )
        }

        response.data.map { it.toDomain() }
    }

    private fun ExpenseDto.toDomain() = Expense(
        id = id,
        businessId = businessId,
        category = runCatching { ExpenseCategory.valueOf(category) }
            .getOrDefault(ExpenseCategory.MISCELLANEOUS),
        amount = amount,
        description = description,
        receiptUrl = receiptUrl,
        recordedAt = Instant.parse(recordedAt),
        expenseDate = LocalDate.parse(expenseDate)
    )

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
