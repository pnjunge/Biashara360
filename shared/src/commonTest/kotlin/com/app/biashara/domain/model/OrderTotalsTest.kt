package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlin.test.*

class OrderTotalsTest {
    private fun order() = Order(
        id = "order", orderNumber = "SALE", businessId = "business", customerId = null,
        customerName = "Customer", customerPhone = "",
        items = listOf(OrderItem("product", "Product", 1, 1000.0, 500.0)),
        paymentStatus = PaymentStatus.PENDING, deliveryStatus = DeliveryStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0)
    )

    @Test fun noTaxIsInventedWhenRateIsUnspecified() {
        assertEquals(1000.0, order().total)
        assertEquals(0.0, order().copy(includeTax = true).taxAmount)
    }

    @Test fun selectedRateIsAppliedOnlyWhenEnabled() {
        assertEquals(1075.0, order().copy(includeTax = true, taxRate = 0.075).total)
        assertEquals(1000.0, order().copy(includeTax = false, taxRate = 0.075).total)
    }

    @Test fun fractionalTaxRoundsToCents() {
        val sale = order().copy(items = listOf(OrderItem("product", "Product", 3, 19.99, 10.0)), includeTax = true, taxRate = 0.16)
        assertEquals(9.6, sale.taxAmount)
        assertEquals(69.57, sale.total)
    }
}
