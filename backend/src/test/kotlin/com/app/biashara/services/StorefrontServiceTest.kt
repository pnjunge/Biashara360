package com.app.biashara.services

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
}
