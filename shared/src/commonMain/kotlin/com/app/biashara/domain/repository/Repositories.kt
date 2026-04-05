package com.app.biashara.domain.repository

import com.app.biashara.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ProductRepository {
    fun getProducts(businessId: String): Flow<List<Product>>
    fun getLowStockProducts(businessId: String): Flow<List<Product>>
    suspend fun getProduct(id: String): Product?
    suspend fun saveProduct(product: Product): Result<Product>
    suspend fun updateStock(productId: String, movement: StockMovement): Result<Unit>
    suspend fun deleteProduct(id: String): Result<Unit>
    fun searchProducts(businessId: String, query: String): Flow<List<Product>>
    fun getStockMovements(productId: String): Flow<List<StockMovement>>
}

interface OrderRepository {
    fun getOrders(businessId: String): Flow<List<Order>>
    fun getOrdersByStatus(businessId: String, status: PaymentStatus): Flow<List<Order>>
    suspend fun getOrder(id: String): Order?
    suspend fun createOrder(order: Order): Result<Order>
    suspend fun updateOrder(order: Order): Result<Order>
    suspend fun updatePaymentStatus(orderId: String, status: PaymentStatus, txCode: String?): Result<Unit>
    suspend fun updateDeliveryStatus(orderId: String, status: DeliveryStatus): Result<Unit>
    fun getOrdersForCustomer(customerId: String): Flow<List<Order>>
    fun getOrdersByDateRange(businessId: String, start: LocalDate, end: LocalDate): Flow<List<Order>>
    suspend fun generateOrderNumber(businessId: String): String
}

interface CustomerRepository {
    fun getCustomers(businessId: String): Flow<List<Customer>>
    fun getTopCustomers(businessId: String, limit: Int = 10): Flow<List<Customer>>
    fun getRepeatCustomers(businessId: String): Flow<List<Customer>>
    suspend fun getCustomer(id: String): Customer?
    suspend fun getCustomerByPhone(phone: String): Customer?
    suspend fun saveCustomer(customer: Customer): Result<Customer>
    suspend fun getCustomerStats(customerId: String): CustomerStats
    fun searchCustomers(businessId: String, query: String): Flow<List<Customer>>
    suspend fun addLoyaltyPoints(customerId: String, points: Int): Result<Unit>
    suspend fun sendMessage(message: CustomerMessage): Result<Unit>
}

interface ExpenseRepository {
    fun getExpenses(businessId: String): Flow<List<Expense>>
    fun getExpensesByCategory(businessId: String, category: ExpenseCategory): Flow<List<Expense>>
    fun getExpensesByDateRange(businessId: String, start: LocalDate, end: LocalDate): Flow<List<Expense>>
    suspend fun saveExpense(expense: Expense): Result<Expense>
    suspend fun deleteExpense(id: String): Result<Unit>
    suspend fun getProfitSummary(businessId: String, period: ReportPeriod): ProfitSummary
}

interface PaymentRepository {
    fun getPayments(businessId: String): Flow<List<Payment>>
    fun getUnreconciledPayments(businessId: String): Flow<List<Payment>>
    suspend fun initiateSTKPush(request: MpesaStkPushRequest): Result<MpesaStkPushResponse>
    suspend fun reconcilePayment(paymentId: String, orderId: String): Result<Unit>
    suspend fun savePayment(payment: Payment): Result<Payment>
    suspend fun getPaymentDashboard(businessId: String): PaymentDashboard
    fun getPaymentsByDateRange(businessId: String, start: LocalDate, end: LocalDate): Flow<List<Payment>>
}

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun verifyOtp(userId: String, otp: String, channel: String): Result<String> // Returns JWT
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun refreshToken(): Result<String>
    suspend fun register(name: String, phone: String, email: String, password: String, businessName: String, businessType: BusinessType): Result<User>
    fun isLoggedIn(): Boolean
}
