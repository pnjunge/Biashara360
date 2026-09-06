package com.app.biashara.services

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.*

class RevenueTrendTest {
    @Test
    fun `daily revenue uses Nairobi midnight and includes days without sales`() {
        val points = buildDailyRevenue(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-03"), listOf(
            Instant.parse("2026-08-31T21:00:00Z") to 200.0,
            Instant.parse("2026-09-01T20:59:59Z") to 300.0,
            Instant.parse("2026-09-02T21:00:00Z") to 100.0,
            Instant.parse("2026-09-03T21:00:00Z") to 999.0
        ))
        assertEquals(listOf("2026-09-01", "2026-09-02", "2026-09-03"), points.map { it.date })
        assertEquals(listOf(500.0, 0.0, 100.0), points.map { it.revenue })
        assertEquals(600.0, points.sumOf { it.revenue })
    }

    @Test
    fun `today and empty ranges retain a zero day`() {
        val today = LocalDate.parse("2026-09-06")
        assertEquals(listOf(0.0), buildDailyRevenue(today, today, emptyList()).map { it.revenue })
        assertFailsWith<IllegalArgumentException> { buildDailyRevenue(today, LocalDate.parse("2026-09-05"), emptyList()) }
    }
}
