package com.app.biashara.presentation.viewmodel

import com.app.biashara.UserSession
import com.app.biashara.data.repository.ProductRepositoryImpl
import com.app.biashara.domain.model.Product
import com.app.biashara.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: InventoryFilter = InventoryFilter.ALL,
    val lowStockCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val error: String? = null,
    val lastSyncTime: Long? = null,
    val isSaving: Boolean = false,
    val saveSucceeded: Boolean = false
)

enum class InventoryFilter { ALL, LOW_STOCK, OUT_OF_STOCK }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val productRepository: ProductRepositoryImpl? = null
) : KmpViewModel() {
    private val _state = MutableStateFlow(InventoryState(isLoading = true))
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        scope.launch {
            UserSession.currentUser
                .map { UserSession.getBusinessId() }
                .distinctUntilChanged()
                .flatMapLatest { businessId ->
                    syncProducts(businessId)
                    getProductsUseCase(businessId)
                }
                .collect { products ->
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

    fun loadProducts(businessId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
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
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        // Show cached data immediately, then reconcile it with the same backend
        // used by the web application. Failed requests leave the cache intact.
        syncProducts(businessId)
    }

    /** Sync products from API and update local cache **/
    fun syncProducts(businessId: String) {
        scope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            try {
                if (productRepository != null) {
                    productRepository.syncProductsFromApi(businessId)
                        .onSuccess { products ->
                            val lowStockCount = products.count { it.isLowStock }
                            val totalValue = products.sumOf { it.sellingPrice * it.currentStock }
                            _state.update { state ->
                                state.copy(
                                    isSyncing = false,
                                    products = products,
                                    filteredProducts = applyFilter(products, state.searchQuery, state.selectedFilter),
                                    lowStockCount = lowStockCount,
                                    totalStockValue = totalValue,
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
            _state.update { it.copy(isSaving = true, saveSucceeded = false, error = null) }
            try {
                saveProductUseCase(product).fold(
                    onSuccess = {
                        _state.update { it.copy(isSaving = false, saveSucceeded = true) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isSaving = false, error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun consumeSaveSuccess() {
        _state.update { it.copy(saveSucceeded = false) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun applyFilter(products: List<Product>, query: String, filter: InventoryFilter): List<Product> {
        var result = products
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.sku.contains(query, ignoreCase = true) ||
                it.barcode?.contains(query, ignoreCase = true) == true
            }
        }
        return when (filter) {
            InventoryFilter.ALL -> result
            InventoryFilter.LOW_STOCK -> result.filter { it.isLowStock && !it.isOutOfStock }
            InventoryFilter.OUT_OF_STOCK -> result.filter { it.isOutOfStock }
        }
    }
}
