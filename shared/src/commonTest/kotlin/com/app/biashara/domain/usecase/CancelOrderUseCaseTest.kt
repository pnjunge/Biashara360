package com.app.biashara.domain.usecase

import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.CustomerRepository
import com.app.biashara.domain.repository.OrderRepository
import com.app.biashara.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CancelOrderUseCaseTest {

    private val now = Clock.System.now()

    @Test
    fun testCancelOrderSuccess() = runBlocking {
        // Setup initial data
        val orderId = "order_123"
        val customerId = "cust_456"
        val productId = "prod_789"
        val businessId = "biz_000"

        val order = Order(
            id = orderId,
            orderNumber = "B360-0001",
            businessId = businessId,
            customerId = customerId,
            customerName = "Jane Doe",
            customerPhone = "+254722000000",
            items = listOf(
                OrderItem(
                    productId = productId,
                    productName = "Product A",
                    quantity = 3,
                    unitPrice = 100.0,
                    buyingPrice = 70.0
                )
            ),
            paymentStatus = PaymentStatus.PENDING,
            deliveryStatus = DeliveryStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )

        // Mock repositories
        var cancelOrderCalled = false
        val orderRepo = object : OrderRepository {
            override fun getOrders(businessId: String): Flow<List<Order>> = flowOf(emptyList())
            override fun getOrdersByStatus(businessId: String, status: PaymentStatus): Flow<List<Order>> = flowOf(emptyList())
            override suspend fun getOrder(id: String): Order? = if (id == orderId) order else null
            override suspend fun createOrder(order: Order): Result<Order> = Result.failure(Exception())
            override suspend fun updateOrder(order: Order): Result<Order> = Result.failure(Exception())
            override suspend fun updatePaymentStatus(orderId: String, status: PaymentStatus, txCode: String?): Result<Unit> = Result.failure(Exception())
            override suspend fun updateDeliveryStatus(orderId: String, status: DeliveryStatus): Result<Unit> = Result.failure(Exception())
            override suspend fun cancelOrder(orderId: String): Result<Unit> {
                cancelOrderCalled = true
                return Result.success(Unit)
            }
            override suspend fun voidOrder(orderId: String): Result<Unit> = Result.success(Unit)
            override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = flowOf(emptyList())
            override fun getOrdersByDateRange(businessId: String, start: LocalDate, end: LocalDate): Flow<List<Order>> = flowOf(emptyList())
            override suspend fun generateOrderNumber(businessId: String): String = ""
        }

        var updatedProductId: String? = null
        var addedStockMovement: StockMovement? = null
        val productRepo = object : ProductRepository {
            override fun getProducts(businessId: String): Flow<List<Product>> = flowOf(emptyList())
            override fun getLowStockProducts(businessId: String): Flow<List<Product>> = flowOf(emptyList())
            override suspend fun getProduct(id: String): Product? = null
            override suspend fun saveProduct(product: Product): Result<Product> = Result.failure(Exception())
            override suspend fun updateStock(productId: String, movement: StockMovement): Result<Unit> {
                updatedProductId = productId
                addedStockMovement = movement
                return Result.success(Unit)
            }
            override suspend fun deleteProduct(id: String): Result<Unit> = Result.failure(Exception())
            override fun searchProducts(businessId: String, query: String): Flow<List<Product>> = flowOf(emptyList())
            override fun getStockMovements(productId: String): Flow<List<StockMovement>> = flowOf(emptyList())
        }

        var updatedCustomerId: String? = null
        var addedPoints: Int? = null
        val customerRepo = object : CustomerRepository {
            override fun getCustomers(businessId: String): Flow<List<Customer>> = flowOf(emptyList())
            override fun getTopCustomers(businessId: String, limit: Int): Flow<List<Customer>> = flowOf(emptyList())
            override fun getTopCustomersWithStats(businessId: String, limit: Int): Flow<List<Pair<Customer, CustomerStats>>> = flowOf(emptyList())
            override fun getRepeatCustomers(businessId: String): Flow<List<Customer>> = flowOf(emptyList())
            override suspend fun getCustomer(id: String): Customer? = null
            override suspend fun getCustomerByPhone(phone: String): Customer? = null
            override suspend fun saveCustomer(customer: Customer): Result<Customer> = Result.failure(Exception())
            override suspend fun getCustomerStats(customerId: String): CustomerStats = CustomerStats(customerId, 0, 0.0, 0.0, null)
            override fun searchCustomers(businessId: String, query: String): Flow<List<Customer>> = flowOf(emptyList())
            override suspend fun addLoyaltyPoints(customerId: String, points: Int): Result<Unit> {
                updatedCustomerId = customerId
                addedPoints = points
                return Result.success(Unit)
            }
            override suspend fun sendMessage(message: CustomerMessage): Result<Unit> = Result.failure(Exception())
        }

        // Run Use Case
        val useCase = CancelOrderUseCase(orderRepo, productRepo, customerRepo)
        val result = useCase(orderId)

        // Verify outcomes
        assertTrue(result.isSuccess)
        assertTrue(cancelOrderCalled)
        assertEquals(productId, updatedProductId)
        assertEquals(3, addedStockMovement?.quantity)
        assertEquals(StockMovementType.STOCK_IN, addedStockMovement?.type)
        assertEquals(customerId, updatedCustomerId)
        // subtotal is 300.0, so points awarded originally was 300 / 100 = 3. Reverting points should deduct 3.
        assertEquals(-3, addedPoints)
    }
}
