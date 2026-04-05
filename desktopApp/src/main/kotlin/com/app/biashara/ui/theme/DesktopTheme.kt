package com.app.biashara.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val B360Green = Color(0xFF1B8B34)
val B360GreenDark = Color(0xFF0F5E22)
val B360GreenLight = Color(0xFF4CAF63)
val B360Amber = Color(0xFFFF8C00)
val B360Red = Color(0xFFD32F2F)
val B360Blue = Color(0xFF1565C0)
val B360Surface = Color(0xFFF4F7F5)
val B360SidebarBg = Color(0xFF1A2332)
val B360SidebarSelected = Color(0xFF243447)

private val DesktopColorScheme = lightColorScheme(
    primary = B360Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7ECC4),
    onPrimaryContainer = B360GreenDark,
    secondary = B360Amber,
    onSecondary = Color.White,
    background = B360Surface,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0xFFE0E0E0)
)

@Composable
fun Biashara360DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopColorScheme,
        typography = Typography(),
        content = content
    )
}
