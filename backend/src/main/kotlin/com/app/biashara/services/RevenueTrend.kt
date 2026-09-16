package com.app.biashara.services

import com.app.biashara.models.DailyRevenueResponse
import kotlinx.datetime.*

internal fun buildDailyRevenue(start: LocalDate, end: LocalDate, paidOrders: List<Pair<Instant, Double>>): List<DailyRevenueResponse> {
    require(end >= start) { "endDate must not be before startDate" }
    val zone = TimeZone.of("Africa/Nairobi")
    val totals = paidOrders.groupBy { it.first.toLocalDateTime(zone).date }
        .mapValues { (_, orders) -> orders.sumOf { it.second } }
    return generateSequence(start) { it.plus(1, DateTimeUnit.DAY) }
        .takeWhile { it <= end }
        .map { DailyRevenueResponse(it.toString(), totals[it] ?: 0.0) }.toList()
}
