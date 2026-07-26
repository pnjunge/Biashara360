package com.app.biashara.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {
    @Test
    fun normalizesSupportedKenyanPhoneFormats() {
        assertEquals("254712345678", ValidationUtils.normalizePhoneKE("0712345678"))
        assertEquals("254712345678", ValidationUtils.normalizePhoneKE("+254712345678"))
        assertEquals("254712345678", ValidationUtils.normalizePhoneKE("712345678"))
    }

    @Test
    fun rejectsInvalidPhoneNumbers() {
        assertFalse(ValidationUtils.isValidPhoneKE("071234567"))
        assertFalse(ValidationUtils.isValidPhoneKE("+255712345678"))
        assertFalse(ValidationUtils.isValidPhoneKE("not-a-number"))
    }

    @Test
    fun enforcesStrongPasswordRules() {
        assertTrue(ValidationUtils.isValidPassword("SecureTrade_9"))
        assertFalse(ValidationUtils.isValidPassword("shortA_9"))
        assertFalse(ValidationUtils.isValidPassword("Password_123"))
        assertFalse(ValidationUtils.isValidPassword("NOLOWERCASE_9"))
    }

    @Test
    fun rejectsUnsafeNames() {
        assertTrue(ValidationUtils.isValidPersonName("Amina Wanjiku"))
        assertFalse(ValidationUtils.isValidPersonName("<script>"))
        assertFalse(ValidationUtils.isValidBusinessName("Trader's Shop"))
    }
}
