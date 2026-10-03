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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.app.biashara.data.local.DesktopPreferencesTokenStorage
import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.refreshSessionIdleTimeout
import com.app.biashara.domain.repository.AuthRepository
import com.app.biashara.presentation.viewmodel.AuthViewModel
import com.app.biashara.presentation.viewmodel.AuthStep
import com.app.biashara.ui.screens.*
import com.app.biashara.ui.theme.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.ui.input.key.*
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class HospitalityStatus(val enabled: Boolean = false)
@Serializable
private data class MenuAccess(val enabledMenus: List<String> = emptyList())

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
    // Top-level / Primary
    object Dashboard : AppScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Pos : AppScreen("pos", "Point of Sale", Icons.Default.ShoppingCart)

    // SALES
    object Orders : AppScreen("orders", "Orders", Icons.Default.ReceiptLong)
    object Customers : AppScreen("customers", "Customers", Icons.Default.People)

    // INVENTORY
    object Products : AppScreen("products", "Products", Icons.Default.Category)
    object Inventory : AppScreen("inventory", "Inventory", Icons.Default.Inventory2)
    object Purchases : AppScreen("purchases", "Purchases", Icons.Default.ShoppingBag)
    object Suppliers : AppScreen("suppliers", "Suppliers", Icons.Default.LocalShipping)

    // BUSINESS
    object HotelRooms : AppScreen("hotel_rooms", "Hotel & Rooms", Icons.Default.Hotel)
    object Hospitality : AppScreen("hospitality", "Restaurant & Bar", Icons.Default.Restaurant)
    object Appointments : AppScreen("appointments", "Appointments", Icons.Default.CalendarToday)
    object Bookings : AppScreen("bookings", "Bookings", Icons.Default.ConfirmationNumber)

    // FINANCE
    object Expenses : AppScreen("expenses", "Expenses", Icons.Default.Receipt)
    object Payments : AppScreen("payments", "Payments", Icons.Default.Payments)
    object Reports : AppScreen("reports", "Reports", Icons.Default.BarChart)

    // ADMINISTRATION
    object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)
    object Tax : AppScreen("tax", "Tax & eTIMS", Icons.Default.AccountBalance)
    object KRA : AppScreen("kra", "KRA", Icons.Default.Gavel)
    object Social : AppScreen("social", "Social", Icons.Default.Forum)
    object OpenTabs : AppScreen("open_tabs", "Open Tabs", Icons.Default.ReceiptLong)
    object CyberSource : AppScreen("cybersource", "CyberSource Settings", Icons.Default.CreditCard)
    object Mpesa : AppScreen("mpesa", "M-Pesa Settings", Icons.Default.Phone)
    object ReceiptTemplate : AppScreen("receipt_template", "Receipt Customization", Icons.Default.ReceiptLong)
}

private data class DesktopNavGroup(
    val key: String,
    val label: String,
    val screens: List<AppScreen>
)

private val topNavScreens = listOf(
    AppScreen.Dashboard,
    AppScreen.Pos,
)

private val desktopNavGroups = listOf(
    DesktopNavGroup(
        key = "SALES",
        label = "SALES",
        screens = listOf(AppScreen.Orders, AppScreen.Customers)
    ),
    DesktopNavGroup(
        key = "INVENTORY",
        label = "INVENTORY",
        screens = listOf(AppScreen.Products, AppScreen.Inventory, AppScreen.Purchases, AppScreen.Suppliers)
    ),
    DesktopNavGroup(
        key = "BUSINESS",
        label = "BUSINESS",
        screens = listOf(AppScreen.HotelRooms, AppScreen.Hospitality, AppScreen.Appointments, AppScreen.Bookings)
    ),
    DesktopNavGroup(
        key = "FINANCE",
        label = "FINANCE",
        screens = listOf(AppScreen.Expenses, AppScreen.Payments, AppScreen.Reports)
    ),
    DesktopNavGroup(
        key = "ADMINISTRATION",
        label = "ADMINISTRATION",
        screens = listOf(AppScreen.Settings)
    ),
)

private val appScreens = topNavScreens + desktopNavGroups.flatMap { it.screens }

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

private fun screenShortcut(screen: AppScreen): String? = when (screen) {
    AppScreen.Dashboard -> "1"
    AppScreen.Pos -> "P"
    AppScreen.Orders -> "O"
    AppScreen.Customers -> "U"
    AppScreen.Products -> "Shift+P"
    AppScreen.Inventory -> "I"
    AppScreen.Purchases -> "L"
    AppScreen.Suppliers -> "Shift+S"
    AppScreen.HotelRooms -> "H"
    AppScreen.Hospitality -> "B"
    AppScreen.Appointments -> "A"
    AppScreen.Bookings -> "K"
    AppScreen.Expenses -> "E"
    AppScreen.Payments -> "Y"
    AppScreen.Reports -> "R"
    AppScreen.Settings -> "S"
    else -> null
}

private fun screenBadge(screen: AppScreen): String? = when (screen) {
    AppScreen.Inventory -> "2"
    AppScreen.Orders -> "22"
    else -> null
}

@Composable
private fun DesktopSidebarItem(
    screen: AppScreen,
    isExpanded: Boolean,
    isSelected: Boolean,
    shortcut: String? = null,
    badge: String? = null,
    isProminentPos: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    if (isProminentPos) {
        val bg = if (isSelected) B360Green else Color(0xFFECFDF5)
        val iconColor = if (isSelected) Color.White else Color(0xFF047857)
        val textColor = if (isSelected) Color.White else Color(0xFF047857)
        val border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFA7F3D0))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .then(if (border != null) Modifier.border(border, RoundedCornerShape(10.dp)) else Modifier)
                .background(bg)
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = if (isHovered && shortcut != null) "$shortcutModifier+$shortcut" else "F1",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    } else {
        val bg = when {
            isSelected -> Color(0xFFE6F9F0)
            isHovered -> Color(0xFFF1F5F9)
            else -> Color.Transparent
        }
        val iconColor = if (isSelected) Color(0xFF059669) else Color(0xFF64748B)
        val textColor = if (isSelected) Color(0xFF059669) else Color(0xFF334155)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = iconColor,
                    modifier = Modifier.size(19.dp)
                )
                if (isExpanded) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = screen.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(Modifier.weight(1f))

                    // Operational count badge (always visible)
                    if (badge != null && (!isHovered || shortcut == null)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF047857) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Shortcut badge - ONLY visible on hover to avoid visual noise!
                    if (isHovered && shortcut != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "$shortcutModifier+$shortcut",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Biashara360DesktopApp(
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = remember { inject() }
    val authRepository: AuthRepository = remember { inject() }
    val tokenStorage: TokenStorage = remember { inject() }
    val client: HttpClient = remember { inject() }
    val userSessionState by UserSession.currentUser.collectAsState()
    var sessionWarningSeconds by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!UserSession.isLoggedIn()) {
            // Restore the persisted refresh-token session before deciding which
            // authenticated screens and tenant-scoped data to display.
            authViewModel.loginWithBiometric()
        }
        runCatching {
            refreshSessionIdleTimeout(client)?.let { tokenStorage.saveSessionIdleTimeoutSeconds(it) }
        }
    }

    // Inactivity monitor
    LaunchedEffect(userSessionState?.id) {
        if (userSessionState == null) {
            sessionWarningSeconds = null
            return@LaunchedEffect
        }
        while (isActive) {
            val remaining = tokenStorage.getSessionRemainingMillis()
            sessionWarningSeconds = remaining
                ?.takeIf { it in 1..60_000L }
                ?.let { ((it + 999) / 1000).toInt() }
            if (remaining == 0L) {
                sessionWarningSeconds = null
                authRepository.logout()
                UserSession.clearUser()
                break
            }
            delay(1_000)
        }
    }

    // Capture all user interactions across the desktop window
    DisposableEffect(userSessionState?.id) {
        if (userSessionState == null) return@DisposableEffect onDispose {}
        val listener = java.awt.event.AWTEventListener {
            (tokenStorage as? DesktopPreferencesTokenStorage)?.touchSessionSync()
                ?: scope.launch { tokenStorage.touchSession() }
        }
        val mask = java.awt.AWTEvent.MOUSE_EVENT_MASK or
                   java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK or
                   java.awt.AWTEvent.KEY_EVENT_MASK or
                   java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(listener, mask)
        onDispose {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
        }
    }

    Biashara360DesktopTheme {
        if (userSessionState == null) {
            DesktopAuthFlow(authViewModel)
        } else {
            Biashara360DesktopAppContent(
                onSignOut = {
                    scope.launch {
                        authRepository.logout()
                        UserSession.clearUser()
                    }
                },
                onMinimize = onMinimize,
                onMaximize = onMaximize,
                onClose = onClose
            )
        }

        sessionWarningSeconds?.let { seconds ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Session Expiring", fontWeight = FontWeight.Bold) },
                text = { Text("You will be signed out in $seconds seconds due to inactivity.") },
                confirmButton = {
                    Button(
                        onClick = {
                            sessionWarningSeconds = null
                            (tokenStorage as? DesktopPreferencesTokenStorage)?.touchSessionSync()
                                ?: scope.launch { tokenStorage.touchSession() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                    ) {
                        Text("Stay Signed In", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            sessionWarningSeconds = null
                            scope.launch {
                                authRepository.logout()
                                UserSession.clearUser()
                            }
                        }
                    ) {
                        Text("Sign Out")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Biashara360DesktopAppContent(
    onSignOut: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    navigationViewModel: DesktopNavigationViewModel = remember { inject() },
    dashboardViewModel: com.app.biashara.presentation.viewmodel.DashboardViewModel = remember { inject() },
    client: HttpClient = remember { inject() }
) {
    val tokenStorage: TokenStorage = remember { inject() }
    val currentScreen by navigationViewModel.currentScreen.collectAsState()
    val dashboardState by dashboardViewModel.state.collectAsState()
    var hospitalityEnabled by remember { mutableStateOf(false) }
    var enabledMenus by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
        runCatching { client.get("$BASE_URL/hospitality/status").body<ApiResponse<HospitalityStatus>>() }
            .onSuccess { hospitalityEnabled = it.success && it.data?.enabled == true }
        runCatching { client.get("$BASE_URL/access/me").body<ApiResponse<MenuAccess>>() }
            .onSuccess { if (it.success) enabledMenus = it.data?.enabledMenus?.toSet() }
        runCatching {
            refreshSessionIdleTimeout(client)?.let { tokenStorage.saveSessionIdleTimeoutSeconds(it) }
        }
    }
    val visibleScreens = appScreens.filter { screen ->
        val menu = when (screen) {
            AppScreen.Dashboard -> "DASHBOARD"; AppScreen.Pos -> "POS"; AppScreen.Orders -> "ORDERS"
            AppScreen.Customers -> "CUSTOMERS"; AppScreen.Products -> "INVENTORY"; AppScreen.Inventory -> "INVENTORY"
            AppScreen.Purchases -> "INVENTORY"; AppScreen.Suppliers -> "INVENTORY"
            AppScreen.HotelRooms -> "HOSPITALITY"; AppScreen.Hospitality -> "HOSPITALITY"
            AppScreen.Appointments -> "SERVICES"; AppScreen.Bookings -> "HOSPITALITY"
            AppScreen.Expenses -> "EXPENSES"; AppScreen.Payments -> "PAYMENTS"
            AppScreen.Reports -> "REPORTS"; AppScreen.Tax -> "TAX"; AppScreen.KRA -> "KRA"; AppScreen.Social -> "SOCIAL"
            AppScreen.Settings -> "SETTINGS"; AppScreen.OpenTabs -> "OPEN_TABS"; else -> null
        }
        menu == null || enabledMenus == null || enabledMenus?.contains(menu) == true
    }
    val visibleTopScreens = topNavScreens.filter { it in visibleScreens }
    val visibleNavGroups = desktopNavGroups.map { group ->
        group.copy(screens = group.screens.filter { it in visibleScreens })
    }.filter { it.screens.isNotEmpty() }
    var isExpanded by remember { mutableStateOf(true) }
    var openNavGroups by remember { mutableStateOf(desktopNavGroups.associate { it.key to true }) }
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
                    val isMod = if (currentDesktopPlatform == DesktopPlatform.MACOS) keyEvent.isMetaPressed else keyEvent.isCtrlPressed
                    if (isMod) {
                        when (keyEvent.key) {
                            Key.K -> {
                                focusRequester.requestFocus()
                                true
                            }
                            Key.One -> { navigationViewModel.navigateTo(AppScreen.Dashboard); true }
                            Key.Two, Key.P -> { navigationViewModel.navigateTo(AppScreen.Pos); true }
                            Key.Three, Key.I -> { navigationViewModel.navigateTo(AppScreen.Inventory); true }
                            Key.Four, Key.O -> { navigationViewModel.navigateTo(AppScreen.Orders); true }
                            Key.Five, Key.U -> { navigationViewModel.navigateTo(AppScreen.Customers); true }
                            Key.Six, Key.E -> { navigationViewModel.navigateTo(AppScreen.Expenses); true }
                            Key.Seven, Key.R -> { navigationViewModel.navigateTo(AppScreen.Reports); true }
                            Key.Eight, Key.S -> { navigationViewModel.navigateTo(AppScreen.Settings); true }
                            Key.L -> { navigationViewModel.navigateTo(AppScreen.Purchases); true }
                            Key.Y -> { navigationViewModel.navigateTo(AppScreen.Payments); true }
                            Key.H -> { navigationViewModel.navigateTo(AppScreen.HotelRooms); true }
                            Key.B -> { navigationViewModel.navigateTo(AppScreen.Hospitality); true }
                            Key.A -> { navigationViewModel.navigateTo(AppScreen.Appointments); true }
                            Key.N -> { navigationViewModel.navigateTo(AppScreen.Pos); true }
                            else -> false
                        }
                    } else if (keyEvent.key == Key.F1) {
                        navigationViewModel.navigateTo(AppScreen.Pos)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        val isWideScreen = maxWidth >= 1024.dp

        Column(modifier = Modifier.fillMaxSize()) {
            DesktopTitleBar(
                title = "Biashara360 — Business Management",
                platform = currentDesktopPlatform,
                onMinimize = onMinimize,
                onMaximize = onMaximize,
                onClose = onClose
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isWideScreen) {
                val sidebarWidth = if (isExpanded) 240.dp else 72.dp
                val sidebarUser by UserSession.currentUser.collectAsState()
                Row(modifier = Modifier.width(sidebarWidth).fillMaxHeight()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    ) {
                        // Brand logo / title
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE6F9F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Logo",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isExpanded) {
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Biashara360",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Business Management",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // User profile card
                        if (isExpanded) {
                            val displayName = sidebarUser?.name?.ifBlank { null } ?: UserSession.getUserName().ifBlank { null } ?: "kamau Admin"
                            val businessName = "kamau-supplies"
                            val userInitial = displayName.firstOrNull()?.toString()?.uppercase() ?: "K"

                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF86EFAC)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userInitial,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Owner • $businessName",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                        }

                        // Menu items
                        Box(modifier = Modifier.weight(1f)) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                visibleTopScreens.forEach { screen ->
                                    val isPos = screen == AppScreen.Pos
                                    DesktopSidebarItem(
                                        screen = screen,
                                        isExpanded = isExpanded,
                                        isSelected = currentScreen == screen,
                                        shortcut = screenShortcut(screen),
                                        badge = screenBadge(screen),
                                        isProminentPos = isPos,
                                        onClick = { navigationViewModel.navigateTo(screen) }
                                    )
                                }
                                visibleNavGroups.forEach { group ->
                                    if (isExpanded) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 2.dp)
                                                .clickable { openNavGroups = openNavGroups + (group.key to !(openNavGroups[group.key] ?: true)) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = group.label,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Icon(
                                                imageVector = if (openNavGroups[group.key] == true) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Toggle ${group.label}",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (!isExpanded || openNavGroups[group.key] == true) {
                                        group.screens.forEach { screen ->
                                            DesktopSidebarItem(
                                                screen = screen,
                                                isExpanded = isExpanded,
                                                isSelected = currentScreen == screen,
                                                shortcut = screenShortcut(screen),
                                                badge = screenBadge(screen),
                                                onClick = { navigationViewModel.navigateTo(screen) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sync status card at bottom of sidebar
                        if (isExpanded) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFECFDF5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Sync status",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "• All changes saved",
                                            fontSize = 10.sp,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "v2.4.1 • ${System.getProperty("os.arch", "x64")}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Desktop",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        // Bottom Sign Out button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSignOut() }
                                .padding(horizontal = if (isExpanded) 12.dp else 6.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            if (isExpanded) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Sign Out",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

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
                    VerticalDivider(color = Color(0xFFE2E8F0))
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
                                    if (searchQuery.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE2E8F0).copy(alpha = 0.8f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(shortcutModifier, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                                Text("K", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                            }
                                        }
                                    } else {
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

                        if (currentScreen == AppScreen.Pos && isWideScreen) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(16.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(B360Green))
                                        Text("Shift active", color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(16.dp)) {
                                    Text("Terminal 01", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(16.dp)) {
                                    Text("eTIMS ready", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.width(12.dp))
                        }

                        PortalOrdersButton(client)

                        IconButton(onClick = { navigationViewModel.navigateTo(AppScreen.Inventory) }) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
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
                            val displayName = user?.name?.ifBlank { null }
                                ?: user?.email?.substringBefore("@")?.ifBlank { null }
                                ?: UserSession.getUserName().ifBlank { null }
                                ?: "Admin"
                            val userInitial = displayName.firstOrNull()?.toString()?.uppercase() ?: "A"
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userInitial,
                                    color = B360Green,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = displayName,
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
                                    onSignOut()
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
                        AppScreen.Orders -> DesktopOrdersScreen(searchQuery = searchQuery)
                        AppScreen.Customers -> DesktopCustomersScreen(searchQuery = searchQuery)
                        AppScreen.Products -> DesktopProductsScreen(
                            searchQuery = searchQuery,
                            onNavigateToInventory = { navigationViewModel.navigateTo(AppScreen.Inventory) }
                        )
                        AppScreen.Inventory -> DesktopInventoryScreen(
                            searchQuery = searchQuery,
                            onNavigateToPurchases = { navigationViewModel.navigateTo(AppScreen.Purchases) }
                        )
                        AppScreen.Purchases -> DesktopPurchasesScreen(
                            searchQuery = searchQuery,
                            onNavigateToInventory = { navigationViewModel.navigateTo(AppScreen.Inventory) }
                        )
                        AppScreen.Suppliers -> DesktopSuppliersScreen(searchQuery = searchQuery)
                        AppScreen.HotelRooms -> DesktopHotelRoomsScreen()
                        AppScreen.Hospitality -> DesktopHospitalityScreen()
                        AppScreen.Appointments -> DesktopAppointmentsScreen()
                        AppScreen.Bookings -> DesktopBookingsScreen()
                        AppScreen.Expenses -> DesktopExpensesModernScreen()
                        AppScreen.Payments -> DesktopPaymentsModernScreen()
                        AppScreen.Reports -> DesktopReportsLiveScreen()
                        AppScreen.Settings -> DesktopSettingsScreen()
                        AppScreen.Tax -> DesktopTaxModernScreen()
                        AppScreen.KRA -> DesktopKraModernScreen()
                        AppScreen.Social -> DesktopSocialModernScreen()
                        AppScreen.OpenTabs -> DesktopOpenTabsScreen()
                        AppScreen.CyberSource -> DesktopPaymentConfigurationScreen()
                        AppScreen.Mpesa -> DesktopPaymentConfigurationScreen()
                        AppScreen.ReceiptTemplate -> DesktopReceiptTemplateScreen()
                    }
                }
            }
        }

        // Bottom status bar tailored for desktop operations (no duplicate time/battery!)
        DesktopStatusBar(
            syncState = "Local • Synced",
            syncSubtitle = "All changes saved",
            terminalInfo = "Counter 01 • Main Store • eTIMS: Online",
            onOpenCommandPalette = { focusRequester.requestFocus() }
        )
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
    var isPinLoginMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
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

            // Auth Mode Toggle Tabs (Password vs Staff PIN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isPinLoginMode) B360Green else Color.Transparent)
                        .clickable { isPinLoginMode = false; viewModel.dismissError() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Password",
                        fontWeight = if (!isPinLoginMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isPinLoginMode) Color.White else Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPinLoginMode) B360Green else Color.Transparent)
                        .clickable { isPinLoginMode = true; viewModel.dismissError() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Default.Pin,
                            contentDescription = null,
                            tint = if (isPinLoginMode) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Staff PIN",
                            fontWeight = if (isPinLoginMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPinLoginMode) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

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

            if (!isPinLoginMode) {
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
            } else {
                CustomLoginTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { char -> char.isDigit() }.take(6); viewModel.dismissError() },
                    placeholder = "Enter 6-Digit Staff PIN",
                    leadingIcon = Icons.Filled.Pin,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    enabled = !state.isLoading
                )

                Text(
                    "Enter your assigned 6-digit staff PIN for rapid terminal authorization.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Button(
                    onClick = { viewModel.loginWithPin(pin) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !state.isLoading && pin.length == 6
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Pin, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign In with Staff PIN", fontWeight = FontWeight.Bold)
                        }
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
