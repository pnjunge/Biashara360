package com.app.biashara.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = { TextButton(onClick = onLogout) { Text("Sign Out", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                // Profile card
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = B360Green, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("W", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                        }
                        Column {
                            Text("Wanjiru Kamau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("wanjiru@biashara360.co.ke", fontSize = 13.sp, color = Color.Gray)
                            Text("Biashara360ERP Business", fontSize = 12.sp, color = B360Green)
                        }
                    }
                }
            }
            item {
                SettingsSection("Preferences") {
                    SettingsToggleItem("Push Notifications", Icons.Filled.Notifications, notificationsEnabled) { notificationsEnabled = it }
                    SettingsToggleItem("Dark Mode", Icons.Filled.DarkMode, darkMode) { darkMode = it }
                    SettingsToggleItem("Biometric Login", Icons.Filled.Fingerprint, biometricEnabled) { biometricEnabled = it }
                }
            }
            item {
                SettingsSection("Business") {
                    SettingsNavItem("Business Profile", Icons.Filled.Business) {}
                    SettingsNavItem("Users & Permissions", Icons.Filled.ManageAccounts) {}
                    SettingsNavItem("Receipt Template", Icons.Filled.Receipt) {}
                }
            }
            item {
                SettingsSection("Integrations") {
                    SettingsNavItem("M-Pesa Setup", Icons.Filled.PhoneAndroid) {}
                    SettingsNavItem("Card Payments", Icons.Filled.CreditCard) {}
                    SettingsNavItem("KRA eTIMS", Icons.Filled.Assignment) {}
                    SettingsNavItem("Social Channels", Icons.Filled.Share) {}
                }
            }
            item {
                SettingsSection("Account") {
                    SettingsNavItem("Change Password", Icons.Filled.Lock) {}
                    SettingsNavItem("Export Data", Icons.Filled.Download) {}
                    SettingsNavItem("Help & Support", Icons.Filled.HelpOutline) {}
                    SettingsNavItem("Sign Out", Icons.Filled.Logout, tint = Color.Red) { showLogoutDialog = true }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
        Card(shape = RoundedCornerShape(12.dp)) { Column { content() } }
    }
}

@Composable
private fun SettingsToggleItem(label: String, icon: ImageVector, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green))
    }
    Divider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsNavItem(label: String, icon: ImageVector, tint: Color = Color.Gray, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 14.sp, color = tint.takeIf { tint != Color.Gray } ?: Color.Unspecified)
        }
        if (tint != Color.Red) Icon(Icons.Filled.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
    }
    Divider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
}
