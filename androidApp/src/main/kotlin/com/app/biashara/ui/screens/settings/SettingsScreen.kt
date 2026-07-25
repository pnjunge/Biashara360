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
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.presentation.viewmodel.AuthViewModel
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToPayments: (() -> Unit)? = null,
    onNavigateToKra: (() -> Unit)? = null,
    onNavigateToSocial: (() -> Unit)? = null,
    onNavigateToCyberSourceSettings: (() -> Unit)? = null,
    authViewModel: AuthViewModel = kmpViewModel(),
    businessViewModel: BusinessViewModel = kmpViewModel()
) {
    val context = LocalContext.current
    val currentUser by UserSession.currentUser.collectAsState()
    val businessProfileState by businessViewModel.profileState.collectAsState()
    val usersState by businessViewModel.usersState.collectAsState()

    LaunchedEffect(Unit) {
        businessViewModel.loadProfile()
        businessViewModel.loadUsers()
    }

    val authState by authViewModel.state.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    // Dark mode reads/writes from the process-scoped ThemeState
    val darkMode by ThemeState.isDarkMode.collectAsState()
    var biometricEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showBusinessProfileDialog by remember { mutableStateOf(false) }
    var showReceiptTemplateDialog by remember { mutableStateOf(false) }
    var showUsersDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    // Change Password dialog state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

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

    // Observe password change result from VM and show toast
    LaunchedEffect(authState.passwordChangeSuccess, authState.passwordChangeError) {
        if (authState.passwordChangeSuccess) {
            Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
            showChangePasswordDialog = false
            currentPassword = ""; newPassword = ""; confirmPassword = ""
            authViewModel.dismissPasswordChangeResult()
        }
        authState.passwordChangeError?.let { err ->
            passwordError = err
            authViewModel.dismissPasswordChangeResult()
        }
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
                        singleLine = true, enabled = !authState.isChangingPassword,
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
                        singleLine = true, enabled = !authState.isChangingPassword,
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
                        singleLine = true, enabled = !authState.isChangingPassword,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    if (passwordError.isNotBlank()) {
                        Text(passwordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    if (authState.isChangingPassword) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = B360Green)
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
                            else -> authViewModel.changePassword(currentPassword, newPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !authState.isChangingPassword
                ) { Text("Change", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false; passwordError = "" }) { Text("Cancel") }
            }
        )
    }

    if (showBusinessProfileDialog) {
        val profile = businessProfileState.profile
        var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
        var owner by remember(profile) { mutableStateOf(profile?.owner ?: "") }
        var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
        var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
        var type by remember(profile) { mutableStateOf(profile?.type ?: "") }
        var county by remember(profile) { mutableStateOf(profile?.county ?: "") }
        var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
        var kraPin by remember(profile) { mutableStateOf(profile?.kraPin ?: "") }

        AlertDialog(
            onDismissRequest = { showBusinessProfileDialog = false },
            title = { Text("Business Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = county, onValueChange = { county = it }, label = { Text("County") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = kraPin, onValueChange = { kraPin = it }, label = { Text("KRA PIN") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profile != null) {
                            businessViewModel.updateProfile(
                                profile.copy(
                                    name = name, owner = owner, phone = phone, email = email,
                                    type = type, county = county, address = address, kraPin = kraPin
                                )
                            )
                        }
                        showBusinessProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showBusinessProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showReceiptTemplateDialog) {
        val profile = businessProfileState.profile
        var header by remember(profile) { mutableStateOf(profile?.receiptHeader ?: "Welcome to our store!") }
        var footer by remember(profile) { mutableStateOf(profile?.receiptFooter ?: "Thank you for shopping with us!") }
        var showTaxVal by remember(profile) { mutableStateOf(profile?.receiptShowTax ?: true) }
        var showCustVal by remember(profile) { mutableStateOf(profile?.receiptShowCustomer ?: true) }

        AlertDialog(
            onDismissRequest = { showReceiptTemplateDialog = false },
            title = { Text("Receipt Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = header, onValueChange = { header = it }, label = { Text("Receipt Header") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Receipt Footer") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show KRA Tax Breakdown", fontSize = 14.sp)
                        Switch(checked = showTaxVal, onCheckedChange = { showTaxVal = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Customer Details", fontSize = 14.sp)
                        Switch(checked = showCustVal, onCheckedChange = { showCustVal = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profile != null) {
                            businessViewModel.updateProfile(
                                profile.copy(
                                    receiptHeader = header, receiptFooter = footer,
                                    receiptShowTax = showTaxVal, receiptShowCustomer = showCustVal
                                )
                            )
                        }
                        showReceiptTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showReceiptTemplateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showUsersDialog) {
        val users = usersState.users
        AlertDialog(
            onDismissRequest = { showUsersDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Users & Permissions", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showAddUserDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add User", tint = B360Green)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (usersState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (users.isEmpty()) {
                        Text("No users found.", modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        users.forEach { u ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${u.role} • ${if (u.isActive != false) "Active" else "Inactive"}", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(
                                        checked = u.isActive != false,
                                        onCheckedChange = { isActive ->
                                            businessViewModel.toggleUserStatus(u.id, isActive)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUsersDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAddUserDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("STAFF") }
        var roleDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Invite New User", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { roleDropdownExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(expanded = roleDropdownExpanded, onDismissRequest = { roleDropdownExpanded = false }) {
                            DropdownMenuItem(text = { Text("ADMIN") }, onClick = { newRole = "ADMIN"; roleDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("MANAGER") }, onClick = { newRole = "MANAGER"; roleDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("STAFF") }, onClick = { newRole = "STAFF"; roleDropdownExpanded = false })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        businessViewModel.inviteUser(newName, newEmail, newPhone, newRole)
                        showAddUserDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) { Text("Invite", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Cancel") }
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
                    SettingsToggleItem("Dark Mode", Icons.Filled.DarkMode, darkMode) { ThemeState.setDarkMode(it) }
                    SettingsToggleItem("Biometric Login", Icons.Filled.Fingerprint, biometricEnabled) { enabled ->
                        if (enabled) {
                            val biometricManager = BiometricManager.from(context)
                            val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                biometricEnabled = true
                            } else {
                                val errorMsg = when (canAuthenticate) {
                                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware found."
                                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable."
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled. Go to settings to set up fingerprint/face."
                                    else -> "Biometrics not available on this device."
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                biometricEnabled = false
                            }
                        } else {
                            biometricEnabled = false
                        }
                    }
                }
            }
            item {
                SettingsSection("Business") {
                    SettingsNavItem("Business Profile", Icons.Filled.Business) { showBusinessProfileDialog = true }
                    SettingsNavItem("Users & Permissions", Icons.Filled.ManageAccounts) { showUsersDialog = true }
                    SettingsNavItem("Receipt Template", Icons.Filled.Receipt) { showReceiptTemplateDialog = true }
                }
            }
            item {
                SettingsSection("Integrations") {
                    SettingsNavItem("M-Pesa Configuration (Read-only)", Icons.Filled.PhoneAndroid) { onNavigateToCyberSourceSettings?.invoke() }
                    SettingsNavItem("CyberSource Configuration (Read-only)", Icons.Filled.CreditCard) { onNavigateToCyberSourceSettings?.invoke() }
                    SettingsNavItem("KRA eTIMS", Icons.Filled.Assignment) { onNavigateToKra?.invoke() }
                    SettingsNavItem("Social Channels", Icons.Filled.Share) { onNavigateToSocial?.invoke() }
                }
            }
            item {
                SettingsSection("Account") {
                    SettingsNavItem("Change Password", Icons.Filled.Lock) { showChangePasswordDialog = true }
                    SettingsNavItem("Export Data", Icons.Filled.Download) {
                        Toast.makeText(
                            context,
                            "Export coming soon — your data will be emailed to $userEmail",
                            Toast.LENGTH_LONG
                        ).show()
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
