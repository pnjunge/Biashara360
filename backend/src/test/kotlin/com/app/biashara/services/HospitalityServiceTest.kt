package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals

class HospitalityServiceTest {
    @Test
    fun `active tabs include tabs awaiting an M-Pesa payment`() {
        assertEquals(
            listOf("OPEN", "AWAITING_PAYMENT"),
            HospitalityService.ACTIVE_TAB_STATUSES,
        )
    }
}
