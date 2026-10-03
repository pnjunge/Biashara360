package com.app.biashara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green

enum class DesktopPlatform {
    MACOS,
    WINDOWS,
    LINUX
}

val currentDesktopPlatform: DesktopPlatform by lazy {
    val os = System.getProperty("os.name", "").lowercase()
    when {
        os.contains("mac") || os.contains("darwin") -> DesktopPlatform.MACOS
        os.contains("win") -> DesktopPlatform.WINDOWS
        else -> DesktopPlatform.LINUX
    }
}

val shortcutModifier: String
    get() = if (currentDesktopPlatform == DesktopPlatform.MACOS) "⌘" else "Ctrl"

/**
 * OS-adaptive native window title bar.
 * - macOS: Traffic light controls on left, centered clean title, no right-side controls.
 * - Windows: Clean title on left, native Windows minimize/maximize/close on right.
 * - Linux: Clean title on left, Linux desktop controls on right.
 * - Removes the redundant "DESKTOP" badge and OS-level battery/clock.
 */
@Composable
fun DesktopTitleBar(
    title: String = "Biashara360 — Business Management",
    platform: DesktopPlatform = currentDesktopPlatform,
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFFF8FAFC))
            .border(width = 0.5.dp, color = Color(0xFFE2E8F0)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (platform) {
            DesktopPlatform.MACOS -> {
                // macOS traffic lights on far left
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5F56))
                            .border(0.5.dp, Color(0xFFE0443E), CircleShape)
                            .clickable(onClick = onClose)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFBD2E))
                            .border(0.5.dp, Color(0xFFDEA123), CircleShape)
                            .clickable(onClick = onMinimize)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF27C93F))
                            .border(0.5.dp, Color(0xFF1AAB29), CircleShape)
                            .clickable(onClick = onMaximize)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Centered title on macOS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(B360Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(68.dp)) // balance left controls
            }

            DesktopPlatform.WINDOWS -> {
                // Windows layout: Left title with icon, Right window controls
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(B360Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Windows native controls on the right
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WindowsControlButton(
                        onClick = onMinimize,
                        hoverColor = Color(0xFFE2E8F0)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(1.dp)
                                .background(Color(0xFF334155))
                        )
                    }
                    WindowsControlButton(
                        onClick = onMaximize,
                        hoverColor = Color(0xFFE2E8F0)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .border(1.dp, Color(0xFF334155))
                        )
                    }
                    WindowsControlButton(
                        onClick = onClose,
                        hoverColor = Color(0xFFE81123)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            DesktopPlatform.LINUX -> {
                // Linux desktop layout: Left title with icon, Right window controls
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(B360Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable(onClick = onMinimize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("—", fontSize = 9.sp, color = Color(0xFF475569))
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable(onClick = onMaximize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("□", fontSize = 9.sp, color = Color(0xFF475569))
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 9.sp, color = Color(0xFF475569))
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowsControlButton(
    onClick: () -> Unit,
    hoverColor: Color,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (isHovered) hoverColor else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Bottom status bar tailored for merchant workstation workflows.
 * Avoids redundant OS telemetry (battery/clock/wifi) and highlights
 * mission-critical local sync status, terminal identity, and shortcuts.
 */
@Composable
fun DesktopStatusBar(
    syncState: String = "Local • Synced",
    syncSubtitle: String = "All changes saved",
    terminalInfo: String = "Counter 01 • Main Store • eTIMS: Online",
    modifier: Modifier = Modifier,
    onOpenCommandPalette: () -> Unit = {},
    onOpenShortcuts: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFFF8FAFC))
            .border(width = 0.5.dp, color = Color(0xFFE2E8F0))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Local • Synced with green indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Text(
                text = syncState,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "•",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = syncSubtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center: Terminal & eTIMS status
        Text(
            text = terminalInfo,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        // Right: Keyboard shortcuts triggers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenCommandPalette)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$shortcutModifier+K",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Command palette",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }

            Text(
                text = "•",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenShortcuts)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Shortcuts",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
