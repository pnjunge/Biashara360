package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import com.app.biashara.data.repository.PaymentRepositoryImpl

data class PaymentsState(
    val isLoading: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val error: String? = null,
    val isSyncing: Boolean = false
) {
    val totalReconciled: Double get() = payments.filter { it.status == TransactionStatus.SUCCESS }.sumOf { it.amount }
    val totalUnmatched: Double get() = payments.filter { it.status == TransactionStatus.SUCCESS && !it.reconciled }.sumOf { it.amount }
}

class PaymentsViewModel(
    private val getPaymentsUseCase: GetPaymentsUseCase,
    private val reconcilePaymentUseCase: ReconcilePaymentUseCase,
    private val paymentRepository: PaymentRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(PaymentsState(isLoading = true))
    val state: StateFlow<PaymentsState> = _state.asStateFlow()

    private val _reconcileResult = MutableSharedFlow<Result<Unit>>()
    val reconcileResult: SharedFlow<Result<Unit>> = _reconcileResult.asSharedFlow()
    private var paymentsJob: Job? = null

    fun loadPayments() {
        val businessId = UserSession.getBusinessId()
        if (paymentsJob == null) paymentsJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                getPaymentsUseCase(businessId).collect { payments ->
                    _state.update { it.copy(isLoading = false, payments = payments) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        refreshPayments()
    }

    fun refreshPayments() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            val result = paymentRepository?.syncPaymentsFromApi(businessId)
                ?: Result.failure(IllegalStateException("Payment repository unavailable"))
            _state.update { it.copy(isSyncing = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun reconcilePayment(paymentId: String, orderId: String) {
        scope.launch {
            val result = reconcilePaymentUseCase(paymentId, orderId)
            _reconcileResult.emit(result)
            if (result.isSuccess) {
                refreshPayments()
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
