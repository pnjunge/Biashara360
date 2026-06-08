package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.GetProfitSummaryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

import com.app.biashara.data.repository.*

data class ReportsState(
    val isLoading: Boolean = false,
    val profitSummary: ProfitSummary? = null,
    val selectedPeriodLabel: String = "This Month",
    val error: String? = null,
    val isSyncing: Boolean = false
)

class ReportsViewModel(
    private val getProfitSummaryUseCase: GetProfitSummaryUseCase,
    private val orderRepository: OrderRepositoryImpl? = null,
    private val expenseRepository: ExpenseRepositoryImpl? = null,
    private val paymentRepository: PaymentRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(ReportsState(isLoading = true))
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    fun loadReport(periodLabel: String = "This Month") {
        val businessId = UserSession.getBusinessId()
        // Load immediately from current local cache
        loadLocalReport(businessId, periodLabel)
        
        // Sync in background
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                orderRepository?.syncOrdersFromApi(businessId)
            } catch (_: Exception) {}
            try {
                expenseRepository?.syncExpensesFromApi(businessId)
            } catch (_: Exception) {}
            try {
                paymentRepository?.syncPaymentsFromApi(businessId)
            } catch (_: Exception) {}
            
            _state.update { it.copy(isSyncing = false) }
            // Reload report with updated synced data
            loadLocalReport(businessId, periodLabel)
        }
    }

    private fun loadLocalReport(businessId: String, periodLabel: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedPeriodLabel = periodLabel) }
            try {
                val period = buildPeriod(periodLabel)
                val summary = getProfitSummaryUseCase(businessId, period)
                _state.update { it.copy(isLoading = false, profitSummary = summary) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildPeriod(label: String): ReportPeriod {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
        return when (label) {
            "Today" -> ReportPeriod(today, today, label)
            "This Week" -> {
                val dayOfWeek = today.dayOfWeek.ordinal
                val start = today.minus(DatePeriod(days = dayOfWeek))
                ReportPeriod(start, today, label)
            }
            "This Month" -> {
                val start = LocalDate(today.year, today.month, 1)
                ReportPeriod(start, today, label)
            }
            "This Quarter" -> {
                val quarterMonth = ((today.monthNumber - 1) / 3) * 3 + 1
                val start = LocalDate(today.year, quarterMonth, 1)
                ReportPeriod(start, today, label)
            }
            "This Year" -> {
                val start = LocalDate(today.year, 1, 1)
                ReportPeriod(start, today, label)
            }
            else -> {
                val start = LocalDate(today.year, today.month, 1)
                ReportPeriod(start, today, "This Month")
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
