package com.app.biashara.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.presentation.viewmodel.AuthViewModel
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToPayments: (() -> Unit)? = null,
    onNavigateToKra: (() -> Unit)? = null,
    onNavigateToSocial: (() -> Unit)? = null,
    onNavigateToCyberSourceSettings: (() -> Unit)? = null,
    authViewModel: AuthViewModel = kmpViewModel()
) {
    val context = LocalContext.current
    val currentUser by UserSession.currentUser.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showBusinessProfileDialog by remember { mutableStateOf(false) }

    // Change Password dialog state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var passwordSaving by remember { mutableStateOf(false) }

    val userName = currentUser?.name ?: ""
    val userEmail = currentUser?.email ?: ""
    val userInitial = if (userName.isNotBlank()) userName.first().uppercase() else "U"

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.logout()
                    onLogout()
                }) { Text("Sign Out", color = B360Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false; passwordError = "" },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPassword, onValueChange = { currentPassword = it; passwordError = "" },
                        label = { Text("Current Password") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, enabled = !passwordSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it; passwordError = "" },
                        label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, enabled = !passwordSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it; passwordError = "" },
                        label = { Text("Confirm New Password") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, enabled = !passwordSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    if (passwordError.isNotBlank()) {
                        Text(passwordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                                passwordError = "All fields required"
                            newPassword != confirmPassword ->
                                passwordError = "Passwords do not match"
                            newPassword.length < 8 ->
                                passwordError = "Password must be at least 8 characters"
                            else -> {
                                // In a full implementation, call changePassword use case
                                showChangePasswordDialog = false
                                currentPassword = ""; newPassword = ""; confirmPassword = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !passwordSaving
                ) { Text("Change", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false; passwordError = "" }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings / Mipangilio", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(B360Surface).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = B360Green, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(userInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(userName.ifBlank { "User" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            if (userEmail.isNotBlank()) Text(userEmail, fontSize = 13.sp, color = Color(0xFF64748B))
                            currentUser?.role?.let { Text(it.name, fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.Bold) }
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
                    SettingsNavItem("Business Profile", Icons.Filled.Business) { showBusinessProfileDialog = true }
                    SettingsNavItem("Users & Permissions", Icons.Filled.ManageAccounts) {
                        // Navigate to users management screen — future screen
                    }
                    SettingsNavItem("Receipt Template", Icons.Filled.Receipt) {
                        // Navigate to receipt customization — future screen
                    }
                }
            }
            item {
                SettingsSection("Integrations") {
                    SettingsNavItem("M-Pesa Setup", Icons.Filled.PhoneAndroid) { onNavigateToPayments?.invoke() }
                    SettingsNavItem("Card / CyberSource Config", Icons.Filled.CreditCard) { onNavigateToCyberSourceSettings?.invoke() }
                    SettingsNavItem("KRA eTIMS", Icons.Filled.Assignment) { onNavigateToKra?.invoke() }
                    SettingsNavItem("Social Channels", Icons.Filled.Share) { onNavigateToSocial?.invoke() }
                }
            }
            item {
                SettingsSection("Account") {
                    SettingsNavItem("Change Password", Icons.Filled.Lock) { showChangePasswordDialog = true }
                    SettingsNavItem("Export Data", Icons.Filled.Download) {
                        // Trigger export API call — placeholder toast
                    }
                    SettingsNavItem("Help & Support", Icons.Filled.Help) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://biashara360.co.ke/support"))
                        context.startActivity(intent)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = B360Red),
                    border = BorderStroke(1.dp, B360Red.copy(0.2f)),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = B360Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out / Toka", color = B360Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsNavItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), fontSize = 14.sp)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun SettingsToggleItem(label: String, icon: ImageVector, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), fontSize = 14.sp)
        Switch(value, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green))
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}
