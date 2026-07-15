package com.app.biashara.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Biashara360 brand colors — modernized ultra-premium Emerald and Slate
val B360Green = Color(0xFF10B981)
val B360GreenBg = Color(0xFFEBF7EE)
val B360GreenDark = Color(0xFF047857)
val B360GreenLight = Color(0xFF34D399)
val B360Amber = Color(0xFFF59E0B)
val B360AmberLight = Color(0xFFFCD34D)
val B360Red = Color(0xFFEF4444)
val B360Blue = Color(0xFF3B82F6)
val B360Surface = Color(0xFFF8FAFC)
val B360OnSurface = Color(0xFF0F172A)

private val LightColorScheme = lightColorScheme(
    primary = B360Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = B360GreenDark,
    secondary = B360Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    error = B360Red,
    background = B360Surface,
    surface = Color.White,
    onSurface = B360OnSurface,
    outline = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = B360GreenLight,
    onPrimary = Color(0xFF022C22),
    primaryContainer = B360GreenDark,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = B360AmberLight,
    onSecondary = Color(0xFF451A03)
)

@Composable
fun Biashara360Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// Status Colors
val PaidColor = Color(0xFF10B981)
val PendingColor = Color(0xFFF59E0B)
val CODColor = Color(0xFF3B82F6)
val FailedColor = Color(0xFFEF4444)

fun paymentStatusColor(status: String): Color = when (status) {
    "PAID" -> PaidColor
    "PENDING" -> PendingColor
    "COD" -> CODColor
    else -> FailedColor
}
