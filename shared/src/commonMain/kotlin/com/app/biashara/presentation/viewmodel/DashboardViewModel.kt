package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.GetDashboardSummaryUseCase
import com.app.biashara.domain.usecase.GetLowStockAlertsUseCase
import com.app.biashara.domain.usecase.GetOrdersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class DashboardState(
    val isLoading: Boolean = false,
    val businessName: String = "",
    val userName: String = "",
    val todayRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0,
    val netProfit: Double = 0.0,
    val pendingOrders: Int = 0,
    val totalOrders: Int = 0,
    val customerCount: Int = 0,
    val lowStockCount: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val topCustomers: List<Customer> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase,
    private val getOrdersUseCase: GetOrdersUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(DashboardState(isLoading = true))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadDashboard() {
        val businessId = UserSession.getBusinessId()
        _state.update {
            it.copy(
                isLoading = true,
                userName = UserSession.getUserName(),
                businessName = UserSession.getUserName().ifBlank { "My Business" }
            )
        }
        scope.launch {
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
                val monthStart = LocalDate(today.year, today.month, 1)
                val period = ReportPeriod(monthStart, today, "This Month")

                val summary = getDashboardSummaryUseCase(businessId, period)
                _state.update {
                    it.copy(
                        monthRevenue = summary.profitSummary.totalRevenue,
                        netProfit = summary.profitSummary.netProfit
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
        scope.launch {
            try {
                getLowStockAlertsUseCase(businessId).collect { products ->
                    _state.update {
                        it.copy(lowStockProducts = products, lowStockCount = products.size)
                    }
                }
            } catch (_: Exception) { }
        }
        scope.launch {
            try {
                getOrdersUseCase(businessId).collect { orders ->
                    val pendingCount = orders.count { it.paymentStatus == PaymentStatus.PENDING }
                    val recent = orders.take(5)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            totalOrders = orders.size,
                            pendingOrders = pendingCount,
                            recentOrders = recent
                        )
                    }
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
