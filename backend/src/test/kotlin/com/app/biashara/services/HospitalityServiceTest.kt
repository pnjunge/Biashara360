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

    @Test
    fun `retail products do not create preparation tickets`() {
        assertEquals(null, hospitalityStationFor("Clothing"))
        assertEquals(null, hospitalityStationFor("Electronics"))
    }

    @Test
    fun `food and drink categories use their preparation stations`() {
        assertEquals("KITCHEN", hospitalityStationFor("Food & Beverage"))
        assertEquals("BAR", hospitalityStationFor("Beer & Wine"))
    }
}
