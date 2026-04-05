package com.app.biashara.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Biashara360 brand colors — inspired by Kenyan flag greens and warmth
val B360Green = Color(0xFF1B8B34)
val B360GreenDark = Color(0xFF0F5E22)
val B360GreenLight = Color(0xFF4CAF63)
val B360Amber = Color(0xFFFF8C00)
val B360AmberLight = Color(0xFFFFB347)
val B360Red = Color(0xFFD32F2F)
val B360Blue = Color(0xFF1565C0)
val B360Surface = Color(0xFFF7FAF8)
val B360OnSurface = Color(0xFF1C1B1F)

private val LightColorScheme = lightColorScheme(
    primary = B360Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7ECC4),
    onPrimaryContainer = B360GreenDark,
    secondary = B360Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF4A2800),
    error = B360Red,
    background = B360Surface,
    surface = Color.White,
    onSurface = B360OnSurface,
    outline = Color(0xFFBDBDBD)
)

private val DarkColorScheme = darkColorScheme(
    primary = B360GreenLight,
    onPrimary = Color(0xFF003912),
    primaryContainer = B360GreenDark,
    onPrimaryContainer = Color(0xFFB7ECC4),
    secondary = B360AmberLight,
    onSecondary = Color(0xFF3A1A00)
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
val PaidColor = Color(0xFF2E7D32)
val PendingColor = Color(0xFFE65100)
val CODColor = Color(0xFF1565C0)
val FailedColor = Color(0xFFC62828)

fun paymentStatusColor(status: String): Color = when (status) {
    "PAID" -> PaidColor
    "PENDING" -> PendingColor
    "COD" -> CODColor
    else -> FailedColor
}
