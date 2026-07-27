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
    val isSyncing: Boolean = false,
    val lastOperation: OrderOperationResult? = null
) {
    val filteredOrders: List<Order>
        get() = if (selectedTabStatus == null) orders
        else orders.filter { it.paymentStatus == selectedTabStatus }
}

data class OrderOperationResult(
    val orderId: String,
    val action: String,
    val succeeded: Boolean,
    val message: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val orderRepository: OrderRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(OrdersState(isLoading = true))
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    init {
        scope.launch {
            UserSession.currentUser
                .map { UserSession.getBusinessId() }
                .distinctUntilChanged()
                .flatMapLatest { businessId ->
                    val effectiveId = businessId.ifBlank { UserSession.getBusinessId() }
                    syncOrders(effectiveId)
                    getOrdersUseCase(effectiveId)
                }
                .collect { orders ->
                    _state.update { it.copy(isLoading = false, orders = orders) }
                }
        }
    }

    fun loadOrders() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val businessId = UserSession.getBusinessId()
            syncOrders(businessId)
        }
    }

    fun syncOrders(businessId: String = UserSession.getBusinessId()) {
        scope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            val effectiveBusinessId = businessId.ifBlank { UserSession.getBusinessId() }
            val result = orderRepository?.syncOrdersFromApi(effectiveBusinessId)
                ?: Result.failure(IllegalStateException("Order repository unavailable"))
            _state.update { it.copy(isSyncing = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun selectTab(status: PaymentStatus?) {
        _state.update { it.copy(selectedTabStatus = status) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null, lastOperation = null) }
    }

    fun cancelOrder(orderId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, lastOperation = null) }
            val res = cancelOrderUseCase(orderId)
            val message = res.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    error = message,
                    lastOperation = OrderOperationResult(orderId, "cancel", res.isSuccess, message)
                )
            }
        }
    }

    fun amendOrder(orderId: String, paymentStatus: PaymentStatus, deliveryStatus: DeliveryStatus) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, lastOperation = null) }
            val original = _state.value.orders.firstOrNull { it.id == orderId }
            val deliveryResult = orderRepository?.updateDeliveryStatus(orderId, deliveryStatus)
                ?: Result.failure(IllegalStateException("Order repository unavailable"))
            val paymentResult = if (deliveryResult.isSuccess) {
                orderRepository?.updatePaymentStatus(orderId, paymentStatus, null)
                    ?: Result.failure(IllegalStateException("Order repository unavailable"))
            } else deliveryResult
            var finalResult = paymentResult
            if (deliveryResult.isSuccess && paymentResult.isFailure && original != null) {
                val rollback = orderRepository?.updateDeliveryStatus(orderId, original.deliveryStatus)
                    ?: Result.failure(IllegalStateException("Order repository unavailable"))
                if (rollback.isFailure) {
                    finalResult = Result.failure(
                        IllegalStateException(
                            "${paymentResult.exceptionOrNull()?.message}; delivery rollback also failed: ${rollback.exceptionOrNull()?.message}"
                        )
                    )
                }
            }
            val message = finalResult.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    error = message,
                    lastOperation = OrderOperationResult(orderId, "amend", finalResult.isSuccess, message)
                )
            }
        }
    }

    fun voidOrder(orderId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, lastOperation = null) }
            val result = orderRepository?.voidOrder(orderId)
                ?: Result.failure(IllegalStateException("Order repository unavailable"))
            val message = result.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    error = message,
                    lastOperation = OrderOperationResult(orderId, "void", result.isSuccess, message)
                )
            }
        }
    }
}
