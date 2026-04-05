package com.app.biashara.presentation.viewmodel

import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.GetDashboardSummaryUseCase
import com.app.biashara.domain.usecase.GetLowStockAlertsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class DashboardState(
    val isLoading: Boolean = false,
    val businessName: String = "My Business",
    val todayRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0,
    val netProfit: Double = 0.0,
    val pendingOrders: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val topCustomers: List<Customer> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(DashboardState(isLoading = true))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboard(businessId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
                val monthStart = LocalDate(today.year, today.month, 1)
                val period = ReportPeriod(monthStart, today, "This Month")

                val summary = getDashboardSummaryUseCase(businessId, period)
                _state.update {
                    it.copy(
                        isLoading = false,
                        monthRevenue = summary.profitSummary.totalRevenue,
                        netProfit = summary.profitSummary.netProfit
                    )
                }
                // Low stock alerts
                getLowStockAlertsUseCase(businessId).collect { products ->
                    _state.update { it.copy(lowStockProducts = products) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
