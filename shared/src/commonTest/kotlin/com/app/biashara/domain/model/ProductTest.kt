package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductTest {
    private fun product(stock: Int, minimumStock: Int = 5) = Product(
        id = "product-1",
        businessId = "business-1",
        sku = "SKU-1",
        name = "Test product",
        buyingPrice = 10.0,
        sellingPrice = 15.0,
        currentStock = stock,
        lowStockThreshold = minimumStock,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0)
    )

    @Test
    fun stockAtMinimumIsLowStock() {
        assertTrue(product(stock = 5).isLowStock)
    }

    @Test
    fun stockAboveMinimumIsNotLowStock() {
        assertFalse(product(stock = 6).isLowStock)
    }

    @Test
    fun outOfStockIsNotAlsoLowStock() {
        val product = product(stock = 0)

        assertTrue(product.isOutOfStock)
        assertFalse(product.isLowStock)
    }
}
