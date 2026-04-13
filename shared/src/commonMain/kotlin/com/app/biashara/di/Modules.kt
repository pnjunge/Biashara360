package com.app.biashara.di

import com.app.biashara.data.local.DatabaseDriverFactory
import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.createHttpClient
import com.app.biashara.data.repository.*
import com.app.biashara.db.Biashara360Database
import com.app.biashara.domain.repository.*
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
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single<CustomerRepository> { CustomerRepositoryImpl(get()) }
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // Use Cases — Inventory
    factory { GetProductsUseCase(get()) }
    factory { GetLowStockAlertsUseCase(get()) }
    factory { SaveProductUseCase(get()) }
    factory { RestockProductUseCase(get()) }

    // Use Cases — Orders
    factory { GetOrdersUseCase(get()) }
    factory { GetOrderUseCase(get()) }
    factory { GetOrdersByStatusUseCase(get()) }
    factory { CreateOrderUseCase(get(), get(), get()) }

    // Use Cases — Customers
    factory { GetCustomersUseCase(get()) }
    factory { GetCustomerUseCase(get()) }
    factory { GetCustomerStatsUseCase(get()) }
    factory { SaveCustomerUseCase(get()) }
    factory { SearchCustomersUseCase(get()) }

    // Use Cases — Expenses
    factory { GetExpensesUseCase(get()) }
    factory { SaveExpenseUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }
    factory { GetProfitSummaryUseCase(get()) }

    // Use Cases — Payments
    factory { GetPaymentsUseCase(get()) }
    factory { GetUnreconciledPaymentsUseCase(get()) }
    factory { ReconcilePaymentUseCase(get()) }
    factory { InitiatePaymentUseCase(get(), get()) }

    // Use Cases — Dashboard
    factory { GetDashboardSummaryUseCase(get(), get(), get(), get()) }

    // Use Cases — Auth
    factory { LoginUseCase(get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // ViewModels
    factory { DashboardViewModel(get(), get(), get()) }
    factory { InventoryViewModel(get(), get(), get()) }
    factory { OrdersViewModel(get()) }
    factory { CustomersViewModel(get(), get(), get(), get()) }
    factory { ExpensesViewModel(get(), get(), get()) }
    factory { PaymentsViewModel(get(), get()) }
    factory { ReportsViewModel(get()) }
    factory { AuthViewModel(get(), get(), get(), get()) }
}

expect val platformModule: Module
