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

data class ReportsState(
    val isLoading: Boolean = false,
    val profitSummary: ProfitSummary? = null,
    val selectedPeriodLabel: String = "This Month",
    val error: String? = null
)

class ReportsViewModel(
    private val getProfitSummaryUseCase: GetProfitSummaryUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ReportsState(isLoading = true))
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    fun loadReport(periodLabel: String = "This Month") {
        val businessId = UserSession.getBusinessId()
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
