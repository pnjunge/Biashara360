package com.app.biashara.services

import com.app.biashara.db.BusinessesTable
import com.app.biashara.db.OrdersTable
import com.app.biashara.db.HospitalityTablesTable
import com.app.biashara.db.ProductsTable
import com.app.biashara.db.BusinessServicesTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

internal fun normalizeStorefrontPhone(value: String): String? {
    val digits = value.trim().replace(Regex("[\\s()-]"), "").removePrefix("+")
    val normalized = when {
        digits.matches(Regex("^0[17]\\d{8}$")) -> "254${digits.drop(1)}"
        digits.matches(Regex("^[17]\\d{8}$")) -> "254$digits"
        digits.matches(Regex("^254[17]\\d{8}$")) -> digits
        else -> null
    }
    return normalized?.takeUnless { it.drop(4).all { digit -> digit == '0' } }
}

internal fun normalizeStorefrontPaymentMethod(value: String): String? =
    value.trim().uppercase().takeIf { it in setOf("MPESA", "COD", "CARD") }

class StorefrontService(
    private val orderService: OrderService,
    private val mpesaService: MpesaService,
    private val hospitalityService: HospitalityService
) {
    fun getStorefront(storeIdentifier: String): StorefrontResponse? = transaction {
        val business = BusinessesTable.select {
            ((BusinessesTable.storefrontSlug eq storeIdentifier) or (BusinessesTable.id eq storeIdentifier)) and
                (BusinessesTable.isActive eq true) and
                (BusinessesTable.subscriptionEnabled eq true)
        }.firstOrNull() ?: return@transaction null
        val businessId = business[BusinessesTable.id]
        val products = ProductsTable.select {
            (ProductsTable.businessId eq businessId) and
                (ProductsTable.isActive eq true) and
                (ProductsTable.currentStock greater 0)
        }.orderBy(ProductsTable.name).map {
            StorefrontProductResponse(
                id = it[ProductsTable.id],
                sku = it[ProductsTable.sku],
                name = it[ProductsTable.name],
                description = it[ProductsTable.description],
                sellingPrice = it[ProductsTable.sellingPrice],
                availableQuantity = it[ProductsTable.currentStock],
                category = it[ProductsTable.category],
                imageUrl = it[ProductsTable.imageUrl]
            )
        }
        StorefrontResponse(
            businessId = businessId,
            storefrontSlug = business[BusinessesTable.storefrontSlug],
            businessName = business[BusinessesTable.name],
            businessType = business[BusinessesTable.type],
            county = business[BusinessesTable.county],
            address = business[BusinessesTable.address],
            currency = business[BusinessesTable.currency],
            welcomeMessage = business[BusinessesTable.receiptHeader],
            themeColor = business[BusinessesTable.storefrontThemeColor],
            headline = business[BusinessesTable.storefrontHeadline],
            description = business[BusinessesTable.storefrontDescription],
            bannerUrl = business[BusinessesTable.storefrontBannerUrl],
            layout = business[BusinessesTable.storefrontLayout],
            tables = if (business[BusinessesTable.hospitalityEnabled]) {
                HospitalityTablesTable.select {
                    (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.isActive eq true)
                }.orderBy(HospitalityTablesTable.name).map {
                    StorefrontTableResponse(it[HospitalityTablesTable.id], it[HospitalityTablesTable.name], it[HospitalityTablesTable.area])
                }
            } else emptyList(),
            services = BusinessServicesTable.select {
                (BusinessServicesTable.businessId eq businessId) and (BusinessServicesTable.isActive eq true)
            }.orderBy(BusinessServicesTable.name).map {
                ServiceCatalogResponse(
                    id = it[BusinessServicesTable.id], name = it[BusinessServicesTable.name], description = it[BusinessServicesTable.description],
                    category = it[BusinessServicesTable.category], durationMinutes = it[BusinessServicesTable.durationMinutes], price = it[BusinessServicesTable.price],
                    isActive = it[BusinessServicesTable.isActive], createdAt = it[BusinessServicesTable.createdAt].toString(), updatedAt = it[BusinessServicesTable.updatedAt].toString(),
                )
            },
            products = products
        )
    }

    suspend fun checkout(storeIdentifier: String, req: StorefrontCheckoutRequest): ApiResponse<StorefrontCheckoutResponse> {
        val validationError = validateStorefrontCheckout(req)
        if (validationError != null) return ApiResponse(false, message = validationError)
        val paymentMethod = normalizeStorefrontPaymentMethod(req.paymentMethod)
            ?: return ApiResponse(false, message = "Payment method must be M-Pesa, card, or cash on delivery")
        val phone = normalizeStorefrontPhone(req.customerPhone)
            ?: return ApiResponse(false, message = "Enter a valid Kenyan mobile number")
        val businessId = resolveActiveBusinessId(storeIdentifier)
            ?: return ApiResponse(false, message = "Store not found")
        val pricedItems = transaction {
            val businessActive = BusinessesTable.select {
                (BusinessesTable.id eq businessId) and
                    (BusinessesTable.isActive eq true) and
                    (BusinessesTable.subscriptionEnabled eq true)
            }.any()
            if (!businessActive) return@transaction null
            val requestedIds = req.items.map { it.productId }
            val products = ProductsTable.select {
                (ProductsTable.businessId eq businessId) and
                    (ProductsTable.id inList requestedIds) and
                    (ProductsTable.isActive eq true)
            }.associateBy { it[ProductsTable.id] }
            if (products.size != requestedIds.distinct().size) return@transaction null
            req.items.map { item ->
                val product = products.getValue(item.productId)
                OrderItemRequest(item.productId, item.quantity, product[ProductsTable.sellingPrice])
            }
        } ?: return ApiResponse(false, message = "One or more products are unavailable")

        val orderResult = if (req.tableId != null) hospitalityService.createOrder(
            businessId, null,
            HospitalityOrderRequest(tableId = req.tableId, guestCount = req.guestCount,
                customerName = req.customerName.trim(), customerPhone = phone, notes = req.notes.trim().take(500), items = pricedItems),
            clientPlatform = "ECOMMERCE", clientReference = req.clientReference, paymentMethod = paymentMethod
        ) else orderService.create(
            businessId,
            CreateOrderRequest(
                clientReference = req.clientReference,
                customerName = req.customerName.trim(),
                customerPhone = phone,
                deliveryLocation = req.deliveryLocation.trim(),
                items = pricedItems,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == "COD") "COD" else "PENDING",
                deliveryStatus = "PENDING",
                notes = req.notes.trim().take(500)
            ),
            clientPlatform = "ECOMMERCE"
        )
        val order = orderResult.data ?: return ApiResponse(false, message = orderResult.message)
        if (paymentMethod == "MPESA") {
            return initiatePayment(businessId, order.id, req.clientReference, phone)
        }
        return ApiResponse(true, data = StorefrontCheckoutResponse(
            orderId = order.id,
            orderNumber = order.orderNumber,
            clientReference = req.clientReference,
            amount = order.subtotal,
            paymentStatus = order.paymentStatus,
            paymentMethod = paymentMethod,
            customerMessage = if (paymentMethod == "COD")
                if (req.tableId != null) "Order sent to your table. Please pay your server after your meal." else "Order placed. Pay when your order is delivered."
            else "Continue to secure card payment."
        ))
    }

    suspend fun retryPayment(
        storeIdentifier: String,
        orderId: String,
        clientReference: String,
        phoneInput: String
    ): ApiResponse<StorefrontCheckoutResponse> {
        val phone = normalizeStorefrontPhone(phoneInput)
            ?: return ApiResponse(false, message = "Enter a valid Kenyan mobile number")
        val businessId = resolveActiveBusinessId(storeIdentifier)
            ?: return ApiResponse(false, message = "Store not found")
        return initiatePayment(businessId, orderId, clientReference, phone)
    }

    fun getOrderStatus(storeIdentifier: String, orderId: String, clientReference: String): StorefrontOrderStatusResponse? = transaction {
        val businessId = resolveActiveBusinessId(storeIdentifier) ?: return@transaction null
        val order = OrdersTable.select {
            (OrdersTable.id eq orderId) and
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.clientReference eq clientReference)
        }.firstOrNull() ?: return@transaction null
        StorefrontOrderStatusResponse(
            orderId = orderId,
            orderNumber = order[OrdersTable.orderNumber],
            amount = order[OrdersTable.subtotal],
            paymentStatus = order[OrdersTable.paymentStatus],
            deliveryStatus = order[OrdersTable.deliveryStatus]
        )
    }

    fun resolveActiveBusinessId(storeIdentifier: String): String? = transaction {
        BusinessesTable.select {
            ((BusinessesTable.storefrontSlug eq storeIdentifier) or (BusinessesTable.id eq storeIdentifier)) and
                (BusinessesTable.isActive eq true) and
                (BusinessesTable.subscriptionEnabled eq true)
        }.firstOrNull()?.get(BusinessesTable.id)
    }

    private suspend fun initiatePayment(
        businessId: String,
        orderId: String,
        clientReference: String,
        phone: String
    ): ApiResponse<StorefrontCheckoutResponse> {
        val order = transaction {
            OrdersTable.select {
                (OrdersTable.id eq orderId) and
                    (OrdersTable.businessId eq businessId) and
                    (OrdersTable.clientReference eq clientReference)
            }.firstOrNull()
        } ?: return ApiResponse(false, message = "Order not found")
        if (order[OrdersTable.paymentStatus] == "PAID") {
            return ApiResponse(false, message = "This order is already paid")
        }
        val result = mpesaService.initiateSTKPush(
            phoneNumber = phone,
            amount = order[OrdersTable.subtotal],
            accountReference = order[OrdersTable.orderNumber],
            transactionDesc = "Store payment for ${order[OrdersTable.orderNumber]}",
            businessId = businessId
        )
        return when (result) {
            is StkPushResult.Success -> {
                orderService.recordMpesaCheckoutAttempt(
                    businessId, orderId, result.checkoutRequestId
                )
                ApiResponse(true, data = StorefrontCheckoutResponse(
                    orderId = orderId,
                    orderNumber = order[OrdersTable.orderNumber],
                    clientReference = clientReference,
                    amount = order[OrdersTable.subtotal],
                    paymentStatus = order[OrdersTable.paymentStatus],
                    paymentMethod = "MPESA",
                    customerMessage = result.customerMessage,
                    checkoutRequestId = result.checkoutRequestId
                ))
            }
            is StkPushResult.Error -> ApiResponse(
                false,
                data = StorefrontCheckoutResponse(
                    orderId = orderId,
                    orderNumber = order[OrdersTable.orderNumber],
                    clientReference = clientReference,
                    amount = order[OrdersTable.subtotal],
                    paymentStatus = order[OrdersTable.paymentStatus],
                    paymentMethod = "MPESA"
                ),
                message = result.message
            )
        }
    }

}

internal fun validateStorefrontCheckout(req: StorefrontCheckoutRequest): String? = when {
        !req.clientReference.matches(Regex("^[A-Za-z0-9._:-]{8,64}$")) -> "Invalid checkout reference"
        req.customerName.trim().length !in 2..100 -> "Customer name must be between 2 and 100 characters"
        req.tableId == null && req.deliveryLocation.trim().length !in 2..500 -> "Enter a delivery or pickup location"
        req.tableId != null && req.tableId.isBlank() -> "Invalid table"
        req.guestCount !in 1..100 -> "Guest count must be between 1 and 100"
        req.items.isEmpty() -> "Your cart is empty"
        req.items.size > 50 -> "A maximum of 50 different products is allowed"
        req.items.map { it.productId }.distinct().size != req.items.size -> "Duplicate products are not allowed"
        req.items.any { it.quantity !in 1..100 } -> "Product quantity must be between 1 and 100"
        else -> null
    }
