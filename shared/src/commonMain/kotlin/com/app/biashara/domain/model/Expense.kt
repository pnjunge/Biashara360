package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val businessId: String,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String,
    val receiptUrl: String? = null,
    val recordedAt: Instant,
    val expenseDate: LocalDate
)

@Serializable
enum class ExpenseCategory {
    ADVERTISING, PACKAGING, DELIVERY, RENT, UTILITIES,
    SALARIES, STOCK_PURCHASE, EQUIPMENT, TRANSPORT, MISCELLANEOUS;

    fun displayName(): String = when (this) {
        ADVERTISING -> "Advertising / Ads"
        PACKAGING -> "Packaging"
        DELIVERY -> "Delivery"
        RENT -> "Rent"
        UTILITIES -> "Utilities"
        SALARIES -> "Salaries"
        STOCK_PURCHASE -> "Stock Purchase"
        EQUIPMENT -> "Equipment"
        TRANSPORT -> "Transport"
        MISCELLANEOUS -> "Miscellaneous"
    }
}

@Serializable
data class ProfitSummary(
    val businessId: String,
    val period: ReportPeriod,
    val totalRevenue: Double,
    val totalCostOfGoods: Double,
    val grossProfit: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val cashflowIn: Double,
    val cashflowOut: Double,
    val netCashflow: Double get() = cashflowIn - cashflowOut
) {
    val grossMargin: Double get() =
        if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0
    val netMargin: Double get() =
        if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
}

@Serializable
data class ReportPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val label: String   // e.g. "March 2025", "This Week"
)
