package com.app.biashara.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.domain.model.User
import com.app.biashara.domain.model.UserRole
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.*
import com.app.biashara.presentation.viewmodel.AuthViewModel
import com.app.biashara.presentation.viewmodel.AuthStep
import com.app.biashara.ui.screens.*
import com.app.biashara.ui.theme.*

// --- ViewModel-driven Navigation State ---
class DesktopNavigationViewModel : com.app.biashara.presentation.viewmodel.KmpViewModel() {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }
}

sealed class AppScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : AppScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Pos : AppScreen("pos", "Point of Sale", Icons.Default.Storefront)
    object Inventory : AppScreen("inventory", "Inventory", Icons.Default.Inventory)
    object Orders : AppScreen("orders", "Orders", Icons.Default.ShoppingCart)
    object Customers : AppScreen("customers", "Customers", Icons.Default.People)
    object Expenses : AppScreen("expenses", "Expenses", Icons.Default.Receipt)
    object Payments : AppScreen("payments", "Payments", Icons.Default.Payments)
    object CyberSource : AppScreen("cybersource", "CyberSource Settings", Icons.Default.CreditCard)
    object Reports : AppScreen("reports", "Reports", Icons.Default.BarChart)
    object Tax : AppScreen("tax", "Tax", Icons.Default.AccountBalance)
    object KRA : AppScreen("kra", "KRA", Icons.Default.Gavel)
    object Social : AppScreen("social", "Social", Icons.Default.Forum)
    object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)
}

private val appScreens = listOf(
    AppScreen.Dashboard,
    AppScreen.Pos,
    AppScreen.Inventory,
    AppScreen.Orders,
    AppScreen.Customers,
    AppScreen.Expenses,
    AppScreen.Payments,
    AppScreen.CyberSource,
    AppScreen.Reports,
    AppScreen.Tax,
    AppScreen.KRA,
    AppScreen.Social,
    AppScreen.Settings
)

@Composable
fun Biashara360DesktopApp() {
    val authViewModel: AuthViewModel = remember { inject() }
    val userSessionState by UserSession.currentUser.collectAsState()

    Biashara360DesktopTheme {
        if (userSessionState == null) {
            DesktopAuthFlow(authViewModel)
        } else {
            Biashara360DesktopAppContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Biashara360DesktopAppContent(
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 1024.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                val sidebarWidth = if (isExpanded) 240.dp else 72.dp
                Column(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    // Header logo
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6F7F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Logo",
                                tint = B360Green,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isExpanded) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Biashara360",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Menu items
                    Box(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            appScreens.forEach { screen ->
                                val isSelected = currentScreen == screen
                                val bg = if (isSelected) Color(0xFFE6F7F0) else Color.Transparent
                                val iconColor = if (isSelected) B360Green else Color(0xFF64748B)
                                val textColor = if (isSelected) B360Green else Color(0xFF1E293B)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { navigationViewModel.navigateTo(screen) }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isExpanded) {
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                            ),
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Collapse toggle button at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight,
                                contentDescription = "Collapse/Expand Sidebar",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    val user by UserSession.currentUser.collectAsState()
                    var showMenu by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color.White)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (isWideScreen) isExpanded = !isExpanded }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF64748B))
                        }

                        Spacer(Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .width(360.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Search anything...", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        IconButton(onClick = {}) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = B360Red,
                                        contentColor = Color.White
                                    ) {
                                        Text("3", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF64748B))
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "J",
                                    color = B360Green,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = user?.name ?: "John Admin",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Admin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Sign Out") },
                                onClick = {
                                    showMenu = false
                                    UserSession.clearUser()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) }
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar {
                            appScreens.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { navigationViewModel.navigateTo(screen) },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> DesktopDashboardScreen()
                        AppScreen.Pos -> DesktopPosScreen()
                        AppScreen.Inventory -> DesktopInventoryScreen()
                        AppScreen.Orders -> DesktopOrdersScreen()
                        AppScreen.Customers -> DesktopCustomersScreen()
                        AppScreen.Expenses -> DesktopExpensesScreen()
                        AppScreen.Payments -> DesktopPaymentsScreen()
                        AppScreen.CyberSource -> DesktopCyberSourceSettingsScreen()
                        AppScreen.Reports -> DesktopReportsScreen()
                        AppScreen.Tax -> DesktopTaxScreen()
                        AppScreen.KRA -> DesktopKraScreen()
                        AppScreen.Social -> DesktopSocialScreen()
                        AppScreen.Settings -> DesktopSettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAuthFlow(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(B360Surface),
        contentAlignment = Alignment.Center
    ) {
        when (val step = state.step) {
            is AuthStep.Login -> {
                DesktopLoginCard(viewModel, state)
            }
            is AuthStep.Otp -> {
                DesktopOtpCard(viewModel, state, step.userId)
            }
        }
    }
}

@Composable
fun DesktopLoginCard(viewModel: AuthViewModel, state: com.app.biashara.presentation.viewmodel.AuthState) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(460.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(B360Green),
                contentAlignment = Alignment.Center
            ) {
                Text("B360", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome to Biashara360", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.height(4.dp))
                Text("Enterprise Management Platform", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            val errorText = state.error
            if (errorText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.dismissError() }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.dismissError() },
                label = { Text("Email / Username") },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = B360Green, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = !state.isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.dismissError() },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = B360Green, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    IconButton({ passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(18.dp))
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = !state.isLoading
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(8.dp),
                enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }

        }
    }
}

@Composable
fun DesktopOtpCard(viewModel: AuthViewModel, state: com.app.biashara.presentation.viewmodel.AuthState, userId: String) {
    var otp by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("SMS") }

    Card(
        modifier = Modifier
            .width(460.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Security, null, tint = B360Green, modifier = Modifier.size(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Two-Factor Authentication", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.height(4.dp))
                Text("Enter the 6-digit OTP code to continue", color = Color.Gray, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SMS", "Email").forEach { ch ->
                    FilterChip(
                        selected = selectedChannel == ch,
                        onClick = { selectedChannel = ch },
                        label = { Text(ch) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = B360Green.copy(0.12f), selectedLabelColor = B360Green),
                        enabled = !state.isLoading
                    )
                }
            }

            val errorText = state.error
            if (errorText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                label = { Text("6-Digit OTP") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = !state.isLoading
            )

            Button(
                onClick = { viewModel.verifyOtp(otp, selectedChannel) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(8.dp),
                enabled = otp.length == 6 && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Verify & Authenticate", fontWeight = FontWeight.Bold)
                }
            }

            val cooldown = state.otpCooldownSeconds
            TextButton(
                onClick = { if (cooldown == 0) viewModel.resendOtp() },
                enabled = cooldown == 0 && !state.isLoading
            ) {
                Text(
                    if (cooldown > 0) "Resend OTP in ${cooldown}s" else "Resend OTP",
                    color = if (cooldown > 0) Color.Gray else B360Green
                )
            }

            TextButton(onClick = { viewModel.goBackToLogin() }) {
                Text("Back to Sign In", color = Color.Gray)
            }
        }
    }
}

