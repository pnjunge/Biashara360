package com.app.biashara.ui.screens

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.*

class DesktopDateFilterTest {
    @Test fun includesBothBoundariesAndUsesNairobiDate() {
        val range = DesktopDateRange("2026-10-10", "2026-10-11")
        assertTrue(range.contains(LocalDate.parse("2026-10-10")))
        assertTrue(range.contains(Instant.parse("2026-10-11T20:59:59Z")))
        assertFalse(range.contains(Instant.parse("2026-10-11T21:00:00Z")))
        assertFalse(range.contains(LocalDate.parse("2026-10-09")))
    }
    @Test fun invalidAndReversedDatesRejectRecords() {
        listOf(DesktopDateRange("2026-02-30", ""), DesktopDateRange("2026-10-12", "2026-10-11"),
            DesktopDateRange("", "wrong")).forEach {
            assertNotNull(it.error)
            assertFalse(it.contains(LocalDate.parse("2026-10-11")))
        }
    }
    @Test fun supportsOpenEndedAndClearedRanges() {
        val date = LocalDate.parse("2026-10-11")
        assertTrue(DesktopDateRange("2026-10-10", "").contains(date))
        assertTrue(DesktopDateRange("", "2026-10-12").contains(date))
        assertTrue(DesktopDateRange().contains(date))
        assertFalse(DesktopDateRange().active)
    }
}
