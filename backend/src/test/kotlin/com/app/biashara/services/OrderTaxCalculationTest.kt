package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderTaxCalculationTest {
    @Test
    fun `includes selected tax in payable total`() {
        val totals = calculateOrderTotals(1_000.0, includeTax = true, requestedTaxRate = 0.16)

        assertEquals(1_000.0, totals.baseAmount)
        assertEquals(0.16, totals.taxRate)
        assertEquals(160.0, totals.taxAmount)
        assertEquals(1_160.0, totals.total)
    }

    @Test
    fun `excludes tax from payable total`() {
        val totals = calculateOrderTotals(1_000.0, includeTax = false, requestedTaxRate = 0.16)

        assertEquals(0.0, totals.taxRate)
        assertEquals(0.0, totals.taxAmount)
        assertEquals(1_000.0, totals.total)
    }

    @Test
    fun `rejects invalid tax rates`() {
        assertFailsWith<IllegalArgumentException> { calculateOrderTotals(1_000.0, true, 1.01) }
    }
}
