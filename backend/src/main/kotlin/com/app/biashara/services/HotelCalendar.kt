package com.app.biashara.services

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

internal object HotelCalendar {
    data class Event(val uid: String, val arrival: LocalDate, val departure: LocalDate)
    fun parse(value: String): List<Event> {
        val lines = value.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.firstOrNull() == "BEGIN:VCALENDAR" && lines.lastOrNull() == "END:VCALENDAR") { "A complete iCalendar file is required" }
        require(lines.count { it == "BEGIN:VCALENDAR" } == 1 && lines.count { it == "END:VCALENDAR" } == 1) { "Invalid calendar" }
        val events = mutableListOf<Event>(); var fields: MutableMap<String, String>? = null
        fun date(raw: String): LocalDate {
            require(raw.matches(Regex("\\d{8}"))) { "Use all-day calendar events with YYYYMMDD dates; recurring or timed events are not supported" }
            return LocalDate.parse("${raw.take(4)}-${raw.substring(4, 6)}-${raw.takeLast(2)}")
        }
        for (line in lines) {
            when (line) {
                "BEGIN:VEVENT" -> { require(fields == null) { "Nested events are invalid" }; fields = mutableMapOf() }
                "END:VEVENT" -> {
                    val f = fields ?: error("Invalid event")
                    if (f["STATUS"] != "CANCELLED") {
                        val uid = f["UID"] ?: error("Each event requires UID")
                        require(uid.length in 1..255) { "Invalid event UID" }
                        val a = date(f["DTSTART"] ?: error("Event start is missing")); val d = date(f["DTEND"] ?: error("Event end is missing"))
                        require(d > a && a.daysUntil(d) <= 366) { "Invalid event date range" }
                        events += Event(uid, a, d)
                    }
                    fields = null
                }
                else -> if (fields != null) {
                    require(!line.startsWith("BEGIN:") && !line.startsWith("END:")) { "Nested event components are not supported" }
                    val key = line.substringBefore(':').substringBefore(';'); val data = line.substringAfter(':', "")
                    require(key !in setOf("RRULE", "RDATE", "EXDATE", "RECURRENCE-ID", "DURATION")) { "Recurring events are not supported; export expanded all-day bookings" }
                    require(key !in fields || key !in setOf("UID", "DTSTART", "DTEND", "STATUS")) { "Duplicate event field" }
                    fields[key] = data
                }
            }
        }
        require(fields == null && events.map { it.uid }.distinct().size == events.size) { "Incomplete or duplicate calendar events" }
        return events
    }
}
