package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.app.biashara.data.repository.PaymentRepositoryImpl

data class PaymentsState(
    val isLoading: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val error: String? = null,
    val isSyncing: Boolean = false
) {
    val totalReconciled: Double get() = payments.filter { it.reconciled }.sumOf { it.amount }
    val totalUnmatched: Double get() = payments.filter { !it.reconciled }.sumOf { it.amount }
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

    fun loadPayments() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                getPaymentsUseCase(businessId).collect { payments ->
                    _state.update { it.copy(isLoading = false, payments = payments) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        syncPayments(businessId)
    }

    fun syncPayments(businessId: String) {
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                paymentRepository?.syncPaymentsFromApi(businessId)
            } catch (_: Exception) { }
            _state.update { it.copy(isSyncing = false) }
        }
    }

    fun reconcilePayment(paymentId: String, orderId: String) {
        scope.launch {
            val result = reconcilePaymentUseCase(paymentId, orderId)
            _reconcileResult.emit(result)
            if (result.isSuccess) {
                loadPayments()
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
