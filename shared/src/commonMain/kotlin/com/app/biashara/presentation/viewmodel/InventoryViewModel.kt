package com.app.biashara.presentation.viewmodel

import com.app.biashara.domain.model.Product
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: InventoryFilter = InventoryFilter.ALL,
    val lowStockCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val error: String? = null
)

enum class InventoryFilter { ALL, LOW_STOCK, OUT_OF_STOCK }

class InventoryViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase,
    private val saveProductUseCase: SaveProductUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(InventoryState(isLoading = true))
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    fun loadProducts(businessId: String) {
        scope.launch {
            getProductsUseCase(businessId).collect { products ->
                val lowStockCount = products.count { it.isLowStock }
                val totalValue = products.sumOf { it.sellingPrice * it.currentStock }
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        products = products,
                        filteredProducts = applyFilter(products, state.searchQuery, state.selectedFilter),
                        lowStockCount = lowStockCount,
                        totalStockValue = totalValue
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { state ->
            state.copy(
                searchQuery = query,
                filteredProducts = applyFilter(state.products, query, state.selectedFilter)
            )
        }
    }

    fun onFilterChange(filter: InventoryFilter) {
        _state.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredProducts = applyFilter(state.products, state.searchQuery, filter)
            )
        }
    }

    fun saveProduct(product: Product) {
        scope.launch {
            saveProductUseCase(product).onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun applyFilter(products: List<Product>, query: String, filter: InventoryFilter): List<Product> {
        var result = products
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.sku.contains(query, ignoreCase = true)
            }
        }
        return when (filter) {
            InventoryFilter.ALL -> result
            InventoryFilter.LOW_STOCK -> result.filter { it.isLowStock && !it.isOutOfStock }
            InventoryFilter.OUT_OF_STOCK -> result.filter { it.isOutOfStock }
        }
    }
}

// Stub ViewModels for other modules
class OrdersViewModel {
    private val _state = MutableStateFlow(emptyList<Any>())
}

class CustomersViewModel {
    private val _state = MutableStateFlow(emptyList<Any>())
}

class ExpensesViewModel {
    private val _state = MutableStateFlow(emptyList<Any>())
}

class PaymentsViewModel {
    private val _state = MutableStateFlow(emptyList<Any>())
}
