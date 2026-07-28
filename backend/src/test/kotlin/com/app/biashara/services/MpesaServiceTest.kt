package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MpesaServiceTest {
    @Test
    fun `paybill uses CustomerPayBillOnline`() {
        assertEquals("CustomerPayBillOnline", mpesaTransactionType("paybill"))
    }

    @Test
    fun `till STK push uses CustomerPayBillOnline`() {
        assertEquals("CustomerPayBillOnline", mpesaTransactionType("till"))
    }

    @Test
    fun `account type mapping is case and whitespace tolerant`() {
        assertEquals("CustomerPayBillOnline", mpesaTransactionType(" TILL "))
    }

    @Test
    fun `unknown account type cannot silently become till`() {
        assertFailsWith<IllegalArgumentException> {
            mpesaTransactionType("merchant")
        }
    }
}
