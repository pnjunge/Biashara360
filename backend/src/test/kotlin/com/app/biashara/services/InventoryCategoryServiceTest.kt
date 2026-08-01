package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals

class InventoryCategoryServiceTest {
    @Test
    fun `normalizes category whitespace without changing merchant casing`() {
        assertEquals("Cold Beverages", InventoryCategoryService.normalizeName("  Cold   Beverages  "))
        assertEquals("Bar Snacks", InventoryCategoryService.normalizeName("Bar\nSnacks"))
    }
}
