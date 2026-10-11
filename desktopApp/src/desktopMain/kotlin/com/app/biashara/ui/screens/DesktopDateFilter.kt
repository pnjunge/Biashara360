package com.app.biashara.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*

internal data class DesktopDateRange(val start: String = "", val end: String = "") {
    val active: Boolean get() = start.isNotBlank() || end.isNotBlank()
    val startDate: LocalDate? get() = parse(start)
    val endDate: LocalDate? get() = parse(end)
    val error: String? get() = when {
        start.isNotBlank() && startDate == null -> "Enter a valid start date (YYYY-MM-DD)."
        end.isNotBlank() && endDate == null -> "Enter a valid end date (YYYY-MM-DD)."
        startDate != null && endDate != null && startDate!! > endDate!! -> "End date must be on or after start date."
        else -> null
    }
    fun contains(date: LocalDate): Boolean = error == null &&
        (startDate == null || date >= startDate!!) && (endDate == null || date <= endDate!!)
    fun contains(instant: Instant): Boolean = contains(instant.toLocalDateTime(TimeZone.of("Africa/Nairobi")).date)
    private fun parse(value: String): LocalDate? = try { LocalDate.parse(value.trim()) } catch (_: IllegalArgumentException) { null }
}

@Composable
internal fun DesktopDateFilter(range: DesktopDateRange, onChange: (DesktopDateRange) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(range.start, { onChange(range.copy(start = it)) }, label = { Text("Start date") },
                placeholder = { Text("YYYY-MM-DD") }, singleLine = true, isError = range.error != null, modifier = Modifier.width(200.dp))
            OutlinedTextField(range.end, { onChange(range.copy(end = it)) }, label = { Text("End date") },
                placeholder = { Text("YYYY-MM-DD") }, singleLine = true, isError = range.error != null, modifier = Modifier.width(200.dp))
            if (range.active) TextButton(onClick = { onChange(DesktopDateRange()) }) { Text("Clear dates") }
        }
        range.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
