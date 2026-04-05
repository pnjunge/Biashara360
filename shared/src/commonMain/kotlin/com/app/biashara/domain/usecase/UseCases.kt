package com.app.biashara.domain.usecase

import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

// --- Inventory Use Cases ---

class GetProductsUseCase(private val repo: ProductRepository) {
    operator fun invoke(businessId: String) = repo.getProducts(businessId)
}

class GetLowStockAlertsUseCase(private val repo: ProductRepository) {
    operator fun invoke(businessId: String) = repo.getLowStockProducts(businessId)
}

class SaveProductUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(product: Product): Result<Product> {
        if (product.name.isBlank()) return Result.failure(IllegalArgumentException("Product name required"))
        if (product.sellingPrice < product.buyingPrice) {
            // Warning only — still allow
        }
        return repo.saveProduct(product)
    }
}

class RestockProductUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(productId: String, quantity: Int, note: String = ""): Result<Unit> {
        val movement = StockMovement(
            id = generateId(),
            productId = productId,
            businessId = "",
            type = StockMovementType.STOCK_IN,
            quantity = quantity,
            note = note,
            recordedAt = Clock.System.now()
        )
        return repo.updateStock(productId, movement)
    }
}

// --- Order Use Cases ---

class CreateOrderUseCase(
    private val orderRepo: OrderRepository,
    private val productRepo: ProductRepository,
    private val customerRepo: CustomerRepository
) {
    suspend operator fun invoke(order: Order): Result<Order> {
        // Validate items have enough stock
        for (item in order.items) {
            val product = productRepo.getProduct(item.productId)
                ?: return Result.failure(IllegalStateException("Product ${item.productName} not found"))
            if (product.currentStock < item.quantity) {
                return Result.failure(IllegalStateException("Insufficient stock for ${item.productName}"))
            }
        }
        val result = orderRepo.createOrder(order)
        if (result.isSuccess) {
            // Deduct stock for each item
            result.getOrNull()?.items?.forEach { item ->
                val movement = StockMovement(
                    id = generateId(),
                    productId = item.productId,
                    businessId = order.businessId,
                    type = StockMovementType.STOCK_OUT,
                    quantity = item.quantity,
                    orderId = order.id,
                    note = "Order ${order.orderNumber}",
                    recordedAt = Clock.System.now()
                )
                productRepo.updateStock(item.productId, movement)
            }
            // Award loyalty points (1 point per 100 KES)
            order.customerId?.let { cid ->
                val points = (order.subtotal / 100).toInt()
                if (points > 0) customerRepo.addLoyaltyPoints(cid, points)
            }
        }
        return result
    }
}

class InitiatePaymentUseCase(
    private val paymentRepo: PaymentRepository,
    private val orderRepo: OrderRepository
) {
    suspend operator fun invoke(orderId: String, phoneNumber: String): Result<MpesaStkPushResponse> {
        val order = orderRepo.getOrder(orderId)
            ?: return Result.failure(IllegalArgumentException("Order not found"))
        val request = MpesaStkPushRequest(
            businessShortCode = "174379",  // Replaced with actual Daraja shortcode at runtime
            phoneNumber = phoneNumber.normalizePhone(),
            amount = order.subtotal,
            accountReference = order.orderNumber,
            transactionDesc = "Payment for order ${order.orderNumber}"
        )
        return paymentRepo.initiateSTKPush(request)
    }
}

// --- Analytics Use Cases ---

class GetDashboardSummaryUseCase(
    private val orderRepo: OrderRepository,
    private val expenseRepo: ExpenseRepository,
    private val customerRepo: CustomerRepository,
    private val productRepo: ProductRepository
) {
    suspend operator fun invoke(businessId: String, period: ReportPeriod): DashboardSummary {
        val profitSummary = expenseRepo.getProfitSummary(businessId, period)
        return DashboardSummary(
            businessId = businessId,
            period = period,
            profitSummary = profitSummary
        )
    }
}

data class DashboardSummary(
    val businessId: String,
    val period: ReportPeriod,
    val profitSummary: ProfitSummary
)

// --- Helpers ---

internal fun generateId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..20).map { chars.random() }.joinToString("")
}

internal fun String.normalizePhone(): String {
    // Convert 07XX to 2547XX format for Mpesa
    return when {
        startsWith("07") -> "254${substring(1)}"
        startsWith("01") -> "254${substring(1)}"
        startsWith("+254") -> substring(1)
        else -> this
    }
}
