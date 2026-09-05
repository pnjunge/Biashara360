package com.app.biashara.services

import com.app.biashara.models.StorefrontCheckoutRequest
import com.app.biashara.models.StorefrontCheckoutItemRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorefrontServiceTest {
    @Test
    fun `storefront accepts supported payment methods`() {
        assertEquals("MPESA", normalizeStorefrontPaymentMethod("mpesa"))
        assertEquals("CARD", normalizeStorefrontPaymentMethod(" card "))
        assertEquals("COD", normalizeStorefrontPaymentMethod("COD"))
        assertEquals(null, normalizeStorefrontPaymentMethod("CASH"))
    }
    @Test
    fun `normalizes supported Kenyan customer numbers`() {
        assertEquals("254712345678", normalizeStorefrontPhone("0712 345 678"))
        assertEquals("254112345678", normalizeStorefrontPhone("+254 112 345 678"))
        assertEquals("254712345678", normalizeStorefrontPhone("712345678"))
    }

    @Test
    fun `rejects invalid customer numbers`() {
        assertNull(normalizeStorefrontPhone("0700000000"))
        assertNull(normalizeStorefrontPhone("12345"))
    }

    private fun checkout() = StorefrontCheckoutRequest(
        clientReference = "store-test-123", customerName = "Customer", customerPhone = "0712345678",
        deliveryLocation = "", items = listOf(StorefrontCheckoutItemRequest("product-1", 1))
    )

    @Test
    fun `table orders use their table instead of requiring a delivery address`() {
        assertNull(validateStorefrontCheckout(checkout().copy(tableId = "table-1")))
        assertEquals("Enter a delivery or pickup location", validateStorefrontCheckout(checkout()))
        assertNull(validateStorefrontCheckout(checkout().copy(deliveryLocation = "Pickup")))
    }

    @Test
    fun `rejects empty table and invalid guest count`() {
        assertEquals("Invalid table", validateStorefrontCheckout(checkout().copy(tableId = "")))
        assertEquals("Guest count must be between 1 and 100", validateStorefrontCheckout(checkout().copy(tableId = "table-1", guestCount = 0)))
    }

    @Test
    fun `table checkout rejects repeated products and invalid quantities`() {
        val item = StorefrontCheckoutItemRequest("product-1", 1)
        assertEquals("Duplicate products are not allowed", validateStorefrontCheckout(checkout().copy(tableId = "table-1", items = listOf(item, item))))
        assertEquals("Product quantity must be between 1 and 100", validateStorefrontCheckout(checkout().copy(tableId = "table-1", items = listOf(item.copy(quantity = 0)))))
    }
}
