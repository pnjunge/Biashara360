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

import com.app.biashara.data.repository.*

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
    val topCustomers: List<Pair<Customer, CustomerStats>> = emptyList(),
    val weeklyRevenue: List<Pair<String, Double>> = emptyList(),
    val error: String? = null,
    val isSyncing: Boolean = false,
    val selectedPeriod: DashboardPeriod = DashboardPeriod.MONTH,
    val lastUpdatedAt: Instant? = null
)

enum class DashboardPeriod(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("7 Days"),
    MONTH("This Month")
}

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val productRepository: ProductRepositoryImpl? = null,
    private val orderRepository: OrderRepositoryImpl? = null,
    private val expenseRepository: ExpenseRepositoryImpl? = null,
    private val customerRepository: CustomerRepositoryImpl? = null,
    private val paymentRepository: PaymentRepositoryImpl? = null
) : KmpViewModel() {
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
        
        // Load flows immediately from local DB
        observeLocalData(businessId)
        
        // Sync remote data in the background
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                productRepository?.syncProductsFromApi(businessId)
            } catch (_: Exception) {}
            try {
                orderRepository?.syncOrdersFromApi(businessId)
            } catch (_: Exception) {}
            try {
                expenseRepository?.syncExpensesFromApi(businessId)
            } catch (_: Exception) {}
            try {
                customerRepository?.syncCustomersFromApi(businessId)
            } catch (_: Exception) {}
            try {
                paymentRepository?.syncPaymentsFromApi(businessId)
            } catch (_: Exception) {}
            
            _state.update { it.copy(isSyncing = false, lastUpdatedAt = Clock.System.now()) }
            
            // Reload financial stats now that local cache is fresh
            loadFinancialSummary(businessId)
        }
    }

    private fun observeLocalData(businessId: String) {
        loadFinancialSummary(businessId)
        
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

                    // Build last-7-days revenue series from real order data
                    val today = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val weeklyRevenue = (6 downTo 0).map { daysAgo ->
                        val targetDate = today.minus(daysAgo, DateTimeUnit.DAY)
                        val dayRevenue = orders
                            .filter { order ->
                                order.createdAt.toLocalDateTime(
                                    TimeZone.of("Africa/Nairobi")
                                ).date == targetDate && order.paymentStatus == PaymentStatus.PAID
                            }
                            .sumOf { it.subtotal }
                        val label = dayLabels[targetDate.dayOfWeek.ordinal]
                        label to dayRevenue
                    }

                    // Today's revenue
                    val todayRevenue = orders
                        .filter { order ->
                            order.createdAt.toLocalDateTime(
                                TimeZone.of("Africa/Nairobi")
                            ).date == today && order.paymentStatus == PaymentStatus.PAID
                        }
                        .sumOf { it.subtotal }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            totalOrders = orders.size,
                            pendingOrders = pendingCount,
                            recentOrders = recent,
                            weeklyRevenue = weeklyRevenue,
                            todayRevenue = todayRevenue
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        scope.launch {
            try {
                customerRepository?.getTopCustomersWithStats(businessId, 4)?.collect { topList ->
                    _state.update {
                        it.copy(topCustomers = topList)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadFinancialSummary(businessId: String) {
        scope.launch {
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
                val selected = state.value.selectedPeriod
                val start = when (selected) {
                    DashboardPeriod.TODAY -> today
                    DashboardPeriod.LAST_7_DAYS -> today.minus(6, DateTimeUnit.DAY)
                    DashboardPeriod.MONTH -> LocalDate(today.year, today.month, 1)
                }
                val period = ReportPeriod(start, today, selected.label)

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
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun selectPeriod(period: DashboardPeriod) {
        if (period == state.value.selectedPeriod) return
        _state.update { it.copy(selectedPeriod = period) }
        loadFinancialSummary(UserSession.getBusinessId())
    }
}
