package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.data.repository.CustomerRepositoryImpl
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val lastSyncTime: Long? = null
) {
    val filteredCustomers: List<Customer>
        get() = if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
        }

    val repeatCustomers: List<Customer>
        get() = customers.filter { c ->
            customers.count { it.phone == c.phone } > 1
        }
}

data class CustomerDetailState(
    val isLoading: Boolean = false,
    val customer: Customer? = null,
    val stats: CustomerStats? = null,
    val error: String? = null
)

class CustomersViewModel(
    private val getCustomersUseCase: GetCustomersUseCase,
    private val saveCustomerUseCase: SaveCustomerUseCase,
    private val getCustomerUseCase: GetCustomerUseCase,
    private val getCustomerStatsUseCase: GetCustomerStatsUseCase,
    private val customerRepository: CustomerRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(CustomersState(isLoading = true))
    val state: StateFlow<CustomersState> = _state.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerDetailState(isLoading = true))
    val detailState: StateFlow<CustomerDetailState> = _detailState.asStateFlow()

    private val _saveResult = MutableSharedFlow<Result<Customer>>()
    val saveResult: SharedFlow<Result<Customer>> = _saveResult.asSharedFlow()

    fun loadCustomers() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                getCustomersUseCase(businessId).collect { customers ->
                    _state.update { it.copy(isLoading = false, customers = customers) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        syncCustomers()
    }

    /** Sync customers from API and update local cache **/
    fun syncCustomers() {
        val businessId = UserSession.getBusinessId()
        scope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            try {
                if (customerRepository != null) {
                    customerRepository.syncCustomersFromApi(businessId)
                        .onSuccess { customers ->
                            _state.update { state ->
                                state.copy(
                                    isSyncing = false,
                                    customers = customers,
                                    lastSyncTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                )
                            }
                        }
                        .onFailure { e ->
                            _state.update { it.copy(isSyncing = false, error = e.message) }
                        }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSyncing = false, error = e.message) }
            }
        }
    }

    fun loadCustomerDetail(customerId: String) {
        scope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            try {
                val customer = getCustomerUseCase(customerId)
                val stats = getCustomerStatsUseCase(customerId)
                _detailState.update {
                    it.copy(isLoading = false, customer = customer, stats = stats)
                }
            } catch (e: Exception) {
                _detailState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun saveCustomer(customer: Customer) {
        scope.launch {
            try {
                val result = saveCustomerUseCase(customer)
                _saveResult.emit(result)
                if (result.isSuccess) {
                    loadCustomers()
                } else {
                    _state.update { it.copy(error = result.exceptionOrNull()?.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
