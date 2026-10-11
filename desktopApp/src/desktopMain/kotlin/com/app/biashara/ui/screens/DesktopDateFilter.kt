package com.app.biashara.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopDateFilter(range: DesktopDateRange, onChange: (DesktopDateRange) -> Unit) {
    var selectingStart by remember { mutableStateOf<Boolean?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { selectingStart = true }, modifier = Modifier.width(220.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start: ${range.startDate?.toString() ?: "Select date"}")
            }
            OutlinedButton(onClick = { selectingStart = false }, modifier = Modifier.width(220.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("End: ${range.endDate?.toString() ?: "Select date"}")
            }
            if (range.active) TextButton(onClick = { onChange(DesktopDateRange()) }) { Text("Clear dates") }
        }
        range.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    selectingStart?.let { isStart ->
        val existingDate = if (isStart) range.startDate else range.endDate
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = existingDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
            initialDisplayMode = DisplayMode.Picker
        )
        DatePickerDialog(
            onDismissRequest = { selectingStart = null },
            confirmButton = {
                TextButton(enabled = pickerState.selectedDateMillis != null, onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // Material calendars represent dates at UTC midnight, independently of device timezone.
                        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString()
                        onChange(if (isStart) range.copy(start = date) else range.copy(end = date))
                    }
                    selectingStart = null
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { selectingStart = null }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState, showModeToggle = false, title = {
                Text(if (isStart) "Select start date" else "Select end date", modifier = Modifier.padding(start = 24.dp, top = 16.dp))
            })
        }
    }
}
