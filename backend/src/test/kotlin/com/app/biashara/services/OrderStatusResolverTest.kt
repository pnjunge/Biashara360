package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderStatusResolverTest {
    @Test
    fun `in-store cash order is paid and delivered`() {
        val statuses = resolveInitialOrderStatuses("CASH", null, null, "In-Store POS")

        assertEquals("PAID", statuses.payment)
        assertEquals("DELIVERED", statuses.delivery)
    }

    @Test
    fun `cash shipment is paid but remains pending delivery`() {
        val statuses = resolveInitialOrderStatuses("CASH", null, null, "Westlands, Nairobi")

        assertEquals("PAID", statuses.payment)
        assertEquals("PENDING", statuses.delivery)
    }

    @Test
    fun `cash on delivery remains cod and pending delivery`() {
        val statuses = resolveInitialOrderStatuses("COD", null, null, "Kisumu")

        assertEquals("COD", statuses.payment)
        assertEquals("PENDING", statuses.delivery)
    }

    @Test
    fun `explicit delivery state is preserved`() {
        val statuses = resolveInitialOrderStatuses("CASH", "PENDING", "PROCESSING", "")

        assertEquals("PAID", statuses.payment)
        assertEquals("PROCESSING", statuses.delivery)
    }
}
