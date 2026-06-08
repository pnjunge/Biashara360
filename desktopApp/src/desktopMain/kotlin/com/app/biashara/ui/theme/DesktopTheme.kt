package com.app.biashara.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val B360Green = Color(0xFF10B981)
val B360GreenDark = Color(0xFF047857)
val B360GreenLight = Color(0xFF34D399)
val B360Amber = Color(0xFFF59E0B)
val B360Red = Color(0xFFEF4444)
val B360Blue = Color(0xFF3B82F6)
val B360Surface = Color(0xFFF8FAFC)
val B360SidebarBg = Color(0xFF0F172A)
val B360SidebarSelected = Color(0xFF1E293B)

private val DesktopColorScheme = lightColorScheme(
    primary = B360Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = B360GreenDark,
    secondary = B360Amber,
    onSecondary = Color.White,
    background = B360Surface,
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun Biashara360DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopColorScheme,
        typography = Typography(),
        content = content
    )
}
