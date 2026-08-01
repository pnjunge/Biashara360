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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.key.*

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
    object Hospitality : AppScreen("hospitality", "Bar & Restaurant", Icons.Default.TableRestaurant)
    object Inventory : AppScreen("inventory", "Inventory", Icons.Default.Inventory)
    object Orders : AppScreen("orders", "Orders", Icons.Default.ShoppingCart)
    object Customers : AppScreen("customers", "Customers", Icons.Default.People)
    object Expenses : AppScreen("expenses", "Expenses", Icons.Default.Receipt)
    object Payments : AppScreen("payments", "Payments", Icons.Default.Payments)
    object CyberSource : AppScreen("cybersource", "CyberSource Settings", Icons.Default.CreditCard)
    object Mpesa : AppScreen("mpesa", "M-Pesa Settings", Icons.Default.Phone)
    object ReceiptTemplate : AppScreen("receipt_template", "Receipt Customization", Icons.Default.ReceiptLong)
    object Reports : AppScreen("reports", "Reports", Icons.Default.BarChart)
    object Tax : AppScreen("tax", "Tax", Icons.Default.AccountBalance)
    object KRA : AppScreen("kra", "KRA", Icons.Default.Gavel)
    object Social : AppScreen("social", "Social", Icons.Default.Forum)
    object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)
}

private val appScreens = listOf(
    AppScreen.Dashboard,
    AppScreen.Pos,
    AppScreen.Hospitality,
    AppScreen.Inventory,
    AppScreen.Orders,
    AppScreen.Customers,
    AppScreen.Expenses,
    AppScreen.Payments,
    AppScreen.Reports,
    AppScreen.Tax,
    AppScreen.KRA,
    AppScreen.Social,
    AppScreen.Settings
)

private fun isDesktopFingerprintAvailable(): Boolean {
    val osName = System.getProperty("os.name").lowercase()
    return try {
        if (osName.contains("linux")) {
            val whichProcess = Runtime.getRuntime().exec(arrayOf("which", "fprintd-verify"))
            if (whichProcess.waitFor() == 0) return true

            val lsusbProcess = Runtime.getRuntime().exec("lsusb")
            val output = lsusbProcess.inputStream.bufferedReader().use { it.readText() }.lowercase()
            output.contains("fingerprint") || output.contains("biometric") || output.contains("fprint")
        } else if (osName.contains("windows")) {
            val process = Runtime.getRuntime().exec(arrayOf("powershell", "-Command", "Get-PnpDevice -Class Biometric"))
            if (process.waitFor() == 0) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                output.isNotBlank() && !output.contains("No PnP devices")
            } else {
                false
            }
        } else if (osName.contains("mac")) {
            val process = Runtime.getRuntime().exec(arrayOf("bioutil", "-read"))
            process.waitFor() == 0
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun Biashara360DesktopApp() {
    val authViewModel: AuthViewModel = remember { inject() }
    val userSessionState by UserSession.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        if (!UserSession.isLoggedIn()) {
            // Restore the persisted refresh-token session before deciding which
            // authenticated screens and tenant-scoped data to display.
            authViewModel.loginWithBiometric(onSuccess = {})
        }
    }

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
    navigationViewModel: DesktopNavigationViewModel = remember { inject() },
    dashboardViewModel: com.app.biashara.presentation.viewmodel.DashboardViewModel = remember { inject() },
    businessViewModel: com.app.biashara.presentation.viewmodel.BusinessViewModel = remember { inject() }
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsState()
    val dashboardState by dashboardViewModel.state.collectAsState()
    val businessProfileState by businessViewModel.profileState.collectAsState()
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
        businessViewModel.loadProfile()
    }
    val visibleScreens = appScreens.filter { screen ->
        screen != AppScreen.Hospitality || businessProfileState.profile?.hospitalityEnabled == true
    }
    var isExpanded by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (keyEvent.isCtrlPressed) {
                        when (keyEvent.key) {
                            Key.K -> {
                                focusRequester.requestFocus()
                                true
                            }
                            Key.One -> { navigationViewModel.navigateTo(AppScreen.Dashboard); true }
                            Key.Two -> { navigationViewModel.navigateTo(AppScreen.Pos); true }
                            Key.Three -> { navigationViewModel.navigateTo(AppScreen.Inventory); true }
                            Key.Four -> { navigationViewModel.navigateTo(AppScreen.Orders); true }
                            Key.Five -> { navigationViewModel.navigateTo(AppScreen.Customers); true }
                            Key.Six -> { navigationViewModel.navigateTo(AppScreen.Expenses); true }
                            Key.Seven -> { navigationViewModel.navigateTo(AppScreen.Reports); true }
                            Key.Eight -> { navigationViewModel.navigateTo(AppScreen.Settings); true }
                            else -> false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        val isWideScreen = maxWidth >= 1024.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                val sidebarWidth = if (isExpanded) 240.dp else 72.dp
                Column(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(B360SidebarBg)
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
                                .background(Color(0xFF1E293B)),
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
                                color = Color.White
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
                            visibleScreens.forEach { screen ->
                                val isSelected = currentScreen == screen
                                val bg = if (isSelected) B360SidebarSelected else Color.Transparent
                                val iconColor = if (isSelected) B360Green else Color(0xFF94A3B8)
                                val textColor = if (isSelected) Color.White else Color(0xFF94A3B8)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { navigationViewModel.navigateTo(screen) }
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight()
                                                .background(B360Green)
                                                .align(Alignment.CenterStart)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = if (isSelected) 16.dp else 12.dp, end = 12.dp),
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
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
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

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .width(360.dp)
                                .height(40.dp)
                                .focusRequester(focusRequester)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E293B)),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text("Search anything...", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
                                        }
                                        innerTextField()
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.weight(1f))

                        IconButton(onClick = { navigationViewModel.navigateTo(AppScreen.Inventory) }) {
                            BadgedBox(
                                badge = {
                                    if (dashboardState.lowStockCount > 0) {
                                        Badge(
                                            containerColor = B360Red,
                                            contentColor = Color.White
                                        ) {
                                            Text(dashboardState.lowStockCount.toString(), fontSize = 9.sp)
                                        }
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
                            visibleScreens.forEach { screen ->
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
                        AppScreen.Hospitality -> DesktopHospitalityScreen()
                        AppScreen.Inventory -> DesktopInventoryScreen(searchQuery = searchQuery)
                        AppScreen.Orders -> DesktopOrdersScreen(searchQuery = searchQuery)
                        AppScreen.Customers -> DesktopCustomersScreen(searchQuery = searchQuery)
                        AppScreen.Expenses -> DesktopExpensesModernScreen()
                        AppScreen.Payments -> DesktopPaymentsModernScreen()
                        AppScreen.CyberSource -> DesktopPaymentConfigurationScreen()
                        AppScreen.Mpesa -> DesktopPaymentConfigurationScreen()
                        AppScreen.ReceiptTemplate -> DesktopReceiptTemplateScreen()
                        AppScreen.Reports -> DesktopReportsLiveScreen()
                        AppScreen.Tax -> DesktopTaxModernScreen()
                        AppScreen.KRA -> DesktopKraModernScreen()
                        AppScreen.Social -> DesktopSocialModernScreen()
                        AppScreen.Settings -> DesktopSettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAuthBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4FBF7)) // Soft light green brand background
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left soft arc
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.06f),
                radius = width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(-width * 0.05f, height * 0.1f)
            )

            // Outer top-left thin arc border
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.04f),
                radius = width * 0.42f,
                center = androidx.compose.ui.geometry.Offset(-width * 0.05f, height * 0.1f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Bottom-right soft arc
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.05f),
                radius = width * 0.3f,
                center = androidx.compose.ui.geometry.Offset(width * 1.05f, height * 0.85f)
            )

            // Bottom-right outer border
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.03f),
                radius = width * 0.38f,
                center = androidx.compose.ui.geometry.Offset(width * 1.05f, height * 0.85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Top-right dot grid
            val dotSpacing = 24.dp.toPx()
            val dotRadius = 2.dp.toPx()
            val startX = width * 0.82f
            val startY = height * 0.15f
            for (col in 0..5) {
                for (row in 0..8) {
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(startX + col * dotSpacing, startY + row * dotSpacing)
                    )
                }
            }

            // Bottom-left dot grid
            val startX2 = width * 0.05f
            val startY2 = height * 0.6f
            for (col in 0..5) {
                for (row in 0..8) {
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(startX2 + col * dotSpacing, startY2 + row * dotSpacing)
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun CustomLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) B360Green else Color(0xFFE2E8F0)
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (enabled) {
                    focusRequester.requestFocus()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(Color(0xFFF0FDF4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = B360Green,
                modifier = Modifier.size(20.dp)
            )
        }

        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFFE2E8F0))
        )

        // Text Input
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                enabled = enabled,
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                decorationBox = { innerTextField: @Composable () -> Unit ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFF94A3B8), fontSize = 15.sp)
                    }
                    innerTextField()
                }
            )
        }

        if (isPassword && onPasswordToggle != null) {
            IconButton(
                onClick = onPasswordToggle,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DesktopAuthFlow(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    DesktopAuthBackground {
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
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotPassword by remember { mutableStateOf(false) }

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

            CustomLoginTextField(
                value = email,
                onValueChange = { email = it; viewModel.dismissError() },
                placeholder = "Email / Username",
                leadingIcon = Icons.Filled.Person,
                enabled = !state.isLoading
            )

            CustomLoginTextField(
                value = password,
                onValueChange = { password = it; viewModel.dismissError() },
                placeholder = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible },
                enabled = !state.isLoading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = B360Green)
                    )
                    Text("Remember me", fontSize = 14.sp, color = Color(0xFF475569))
                }
                Text(
                    text = "Forgot password?",
                    fontSize = 14.sp,
                    color = B360Green,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showForgotPassword = true }
                )
            }

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(
                    text = "OR",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            OutlinedButton(
                onClick = {
                    if (isDesktopFingerprintAvailable()) {
                        viewModel.setError("Fingerprint login requires prior enrollment. Please ask your administrator to enroll your fingerprint via the admin portal.")
                    } else {
                        viewModel.setError("No fingerprint reader or biometric hardware detected on this system.")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Fingerprint, null, tint = B360Green, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Fingerprint", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To reset your password, contact your system administrator or visit your account portal to initiate a password reset via email.")
                    Text("Your administrator can access the user management section in the backend admin panel.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showForgotPassword = false },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Got it", color = Color.White) }
            }
        )
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
                onClick = { if (cooldown == 0) viewModel.resendOtp(selectedChannel) },
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
