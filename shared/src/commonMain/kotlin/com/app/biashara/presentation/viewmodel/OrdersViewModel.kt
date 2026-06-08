package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.app.biashara.data.repository.OrderRepositoryImpl

data class OrdersState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selectedTabStatus: PaymentStatus? = null,
    val error: String? = null,
    val isSyncing: Boolean = false
) {
    val filteredOrders: List<Order>
        get() = if (selectedTabStatus == null) orders
        else orders.filter { it.paymentStatus == selectedTabStatus }
}

class OrdersViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val orderRepository: OrderRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(OrdersState(isLoading = true))
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    fun loadOrders() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                getOrdersUseCase(businessId).collect { orders ->
                    _state.update { it.copy(isLoading = false, orders = orders) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        syncOrders(businessId)
    }

    fun syncOrders(businessId: String) {
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                orderRepository?.syncOrdersFromApi(businessId)
            } catch (_: Exception) { }
            _state.update { it.copy(isSyncing = false) }
        }
    }

    fun selectTab(status: PaymentStatus?) {
        _state.update { it.copy(selectedTabStatus = status) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun cancelOrder(orderId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val res = cancelOrderUseCase(orderId)
            if (res.isSuccess) {
                _state.update { it.copy(isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = res.exceptionOrNull()?.message) }
            }
        }
    }
}
