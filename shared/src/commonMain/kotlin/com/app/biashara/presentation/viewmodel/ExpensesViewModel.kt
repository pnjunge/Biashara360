package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpensesState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val error: String? = null
) {
    val totalAmount: Double get() = expenses.sumOf { it.amount }
}

class ExpensesViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ExpensesState(isLoading = true))
    val state: StateFlow<ExpensesState> = _state.asStateFlow()

    private val _saveResult = MutableSharedFlow<Result<Expense>>()
    val saveResult: SharedFlow<Result<Expense>> = _saveResult.asSharedFlow()

    fun loadExpenses() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                getExpensesUseCase(businessId).collect { expenses ->
                    _state.update { it.copy(isLoading = false, expenses = expenses) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun saveExpense(expense: Expense) {
        scope.launch {
            val result = saveExpenseUseCase(expense)
            _saveResult.emit(result)
            if (result.isSuccess) {
                loadExpenses()
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun deleteExpense(id: String) {
        scope.launch {
            val result = deleteExpenseUseCase(id)
            if (result.isSuccess) {
                loadExpenses()
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
