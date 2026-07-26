package com.app.biashara.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNormalizationTest {
    @Test
    fun normalizesCommonKenyanMobileFormats() {
        assertEquals("254712345678", "0712345678".normalizeKenyanMobile())
        assertEquals("254712345678", "+254 712 345 678".normalizeKenyanMobile())
        assertEquals("254112345678", "0112345678".normalizeKenyanMobile())
    }

    @Test
    fun rejectsInvalidOrNonKenyanNumbers() {
        assertNull("070000000".normalizeKenyanMobile())
        assertNull("12345".normalizeKenyanMobile())
        assertNull("+255712345678".normalizeKenyanMobile())
    }
}
