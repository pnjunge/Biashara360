package com.app.biashara.di

import com.app.biashara.data.local.DatabaseDriverFactory
import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.createHttpClient
import com.app.biashara.data.repository.ProductRepositoryImpl
import com.app.biashara.db.Biashara360Database
import com.app.biashara.domain.repository.ProductRepository
import com.app.biashara.domain.usecase.*
import com.app.biashara.presentation.viewmodel.*
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule = module {
    // Database
    single { get<DatabaseDriverFactory>().createDriver() }
    single { Biashara360Database(get()) }

    // HTTP Client
    single { createHttpClient(get()) }

    // Repositories
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    // (OrderRepository, CustomerRepository, etc. follow same pattern)

    // Use Cases
    factory { GetProductsUseCase(get()) }
    factory { GetLowStockAlertsUseCase(get()) }
    factory { SaveProductUseCase(get()) }
    factory { RestockProductUseCase(get()) }

    // ViewModels
    factory { DashboardViewModel(get(), get()) }
    factory { InventoryViewModel(get(), get(), get()) }
    factory { OrdersViewModel() }
    factory { CustomersViewModel() }
    factory { ExpensesViewModel() }
    factory { PaymentsViewModel() }
}

expect val platformModule: Module
