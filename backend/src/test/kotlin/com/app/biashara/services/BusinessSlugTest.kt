package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessSlugTest {
    @Test
    fun `normalizes a business name for a public URL`() {
        assertEquals("njunges-fresh-mart", storefrontSlugBase(" Njunge's Fresh Mart "))
    }

    @Test
    fun `uses a safe fallback when the name has no latin URL characters`() {
        assertEquals("business", storefrontSlugBase("***"))
    }

    @Test
    fun `limits slug length for the database column`() {
        assertEquals(100, storefrontSlugBase("a".repeat(150)).length)
    }
}
