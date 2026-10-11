package com.app.biashara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.biashara.ui.navigation.Screen
import com.app.biashara.ui.navigation.bottomNavItems
import com.app.biashara.ui.navigation.primaryBottomNavItems
import com.app.biashara.ui.navigation.secondaryNavItems
import com.app.biashara.ui.screens.auth.LoginScreen
import com.app.biashara.ui.screens.auth.RegisterScreen
import com.app.biashara.ui.screens.customers.CustomersScreen
import com.app.biashara.ui.screens.dashboard.DashboardScreen
import com.app.biashara.ui.screens.expenses.ExpensesScreen
import com.app.biashara.ui.screens.inventory.InventoryScreen
import com.app.biashara.ui.screens.orders.OrdersScreen
import com.app.biashara.ui.screens.payments.PaymentsScreen
import com.app.biashara.ui.screens.tax.TaxScreen
import com.app.biashara.ui.screens.kra.KraScreen
import com.app.biashara.ui.screens.social.SocialScreen
import com.app.biashara.ui.screens.pos.PosScreen
import com.app.biashara.ui.screens.hospitality.HospitalityOperationsScreen
import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.app.biashara.UserSession
import com.app.biashara.data.remote.NativeAccess
import com.app.biashara.data.remote.NativeAccessSession
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import com.app.biashara.ui.screens.auth.SubscriptionActivationScreen
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

val LocalWindowWidthSizeClass = staticCompositionLocalOf<WindowWidthSizeClass> {
    WindowWidthSizeClass.Compact
}

@Serializable
private data class HospitalityStatus(val enabled: Boolean = false)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun Biashara360App(
    windowWidthSizeClass: WindowWidthSizeClass = run {
        val activity = LocalContext.current as? Activity
        if (activity != null) calculateWindowSizeClass(activity).widthSizeClass
        else WindowWidthSizeClass.Compact
    }
) {
    val startDestination = Screen.Login.route
    val tokenStorage = koinInject<TokenStorage>()
    val authRepository = koinInject<AuthRepository>()
    val client = koinInject<HttpClient>()
    val coroutineScope = rememberCoroutineScope()
    var sessionWarningSeconds by remember { mutableStateOf<Int?>(null) }
    val networkAvailable = rememberNetworkAvailable()

    val offlineState by com.app.biashara.data.offline.NativeOffline.state.collectAsState()
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        if (tokenStorage.getAccessToken() != null && !UserSession.isLoggedIn()) {
            authRepository.loginWithBiometric().onSuccess {
                navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } }
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var hospitalityEnabled by remember { mutableStateOf(false) }
    val user by UserSession.currentUser.collectAsState()
    val access by NativeAccessSession.access.collectAsState()
    var accessLoading by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf<String?>(null) }
    var accessReload by remember { mutableStateOf(0) }
    LaunchedEffect(user?.id, user?.businessId, accessReload) {
        NativeAccessSession.reset()
        hospitalityEnabled = false
        if (user == null) return@LaunchedEffect
        accessLoading = true
        accessError = null
        runCatching {
            val result = client.get("$BASE_URL/access/me").body<ApiResponse<NativeAccess>>()
            check(result.success && result.data != null) { result.message.ifBlank { "Unable to load access rights" } }
            NativeAccessSession.set(requireNotNull(result.data))
        }.onFailure { accessError = it.message ?: "Unable to load access rights" }
        accessLoading = false
        runCatching { client.get("$BASE_URL/hospitality/status").body<ApiResponse<HospitalityStatus>>() }
            .onSuccess { hospitalityEnabled = it.success && it.data?.enabled == true }
    }
    if (user != null) key(user?.id, user?.businessId) { NativeLoginShiftPrompt(client, access) }

    LaunchedEffect(currentDestination?.route) {
        while (true) {
            val remaining = tokenStorage.getSessionRemainingMillis()
            sessionWarningSeconds = remaining
                ?.takeIf { it in 1..60_000 }
                ?.let { ((it + 999) / 1000).toInt() }
            if (remaining == 0L) {
                authRepository.logout()
                sessionWarningSeconds = null
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            delay(1_000)
        }
    }

    sessionWarningSeconds?.let { seconds ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Session expiring") },
            text = { Text("You’ll be signed out in $seconds seconds due to inactivity.") },
            confirmButton = {
                Button(
                    onClick = {
                        sessionWarningSeconds = null
                        coroutineScope.launch { tokenStorage.touchSession() }
                    }
                ) { Text("Stay signed in") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sessionWarningSeconds = null
                        coroutineScope.launch {
                            authRepository.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                ) { Text("Sign out") }
            }
        )
    }

    var showMoreSheet by remember { mutableStateOf(false) }

    fun canOpen(screen: Screen, permission: String? = null): Boolean {
        val menu = when (screen) {
            Screen.Dashboard -> "DASHBOARD"; Screen.Pos -> "POS"; Screen.Inventory, Screen.AddProduct -> "INVENTORY"
            Screen.Orders, Screen.OrderDetail, Screen.CreateOrder -> "ORDERS"
            Screen.Customers, Screen.CustomerDetail -> "CUSTOMERS"; Screen.Expenses, Screen.AddExpense -> "EXPENSES"
            Screen.Payments -> "PAYMENTS"; Screen.Social -> "SOCIAL"; Screen.HospitalityOperations -> "HOSPITALITY_OPS"
            Screen.Reports -> "REPORTS"; Screen.Tax -> "TAX"; Screen.Kra -> "KRA"
            Screen.Settings, Screen.PaymentConfiguration, Screen.CyberSourceSettings -> "SETTINGS"
            else -> return false
        }
        val required = permission ?: when (screen) {
            Screen.Pos, Screen.CreateOrder -> "orders.create"
            Screen.Reports -> "reports.financial"
            Screen.Inventory -> "inventory.view"
            Screen.HospitalityOperations -> "hospitality.view"
            else -> null
        }
        // Android combines the restaurant and operations views in one screen.
        val menuAllowed = access?.allows(menu) == true ||
            (screen == Screen.HospitalityOperations && access?.allows("HOSPITALITY") == true)
        return menuAllowed && (required == null || access?.permits(required) == true) &&
            (screen != Screen.HospitalityOperations || hospitalityEnabled)
    }
    fun isMenuEnabled(item: com.app.biashara.ui.navigation.BottomNavItem) = canOpen(item.screen)

    fun goBackFromAccessGate() {
        val fallback = (primaryBottomNavItems + secondaryNavItems).firstOrNull { isMenuEnabled(it) }
        if (fallback != null) {
            navController.navigate(fallback.screen.route) { launchSingleTop = true }
        } else coroutineScope.launch {
            authRepository.logout()
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    val visiblePrimaryNavItems = primaryBottomNavItems.filter { isMenuEnabled(it) }
    val visibleSecondaryNavItems = secondaryNavItems.filter { isMenuEnabled(it) }

    val showBottomBar = (visiblePrimaryNavItems + visibleSecondaryNavItems).any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletOrExpanded = windowWidthSizeClass == WindowWidthSizeClass.Medium || windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val useNavigationRail = isLandscape || isTabletOrExpanded

    if (showMoreSheet) {
        MoreAppsBottomSheet(
            onDismiss = { showMoreSheet = false },
            navController = navController,
            secondaryItems = visibleSecondaryNavItems
        )
    }

    CompositionLocalProvider(
        LocalNetworkAvailable provides networkAvailable,
        LocalWindowWidthSizeClass provides windowWidthSizeClass
    ) {
    Scaffold(
        topBar = {
            Column {
            DesktopOfflineQueueButton()
            if (!networkAvailable || offlineState.pending > 0 || offlineState.error != null) {
                Surface(color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFF92400E))
                        Text(
                            offlineState.error ?: "${if (!networkAvailable) "Offline · " else ""}${offlineState.pending} actions pending sync · Cash/COD available",
                            color = Color(0xFF92400E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            }
        },
        bottomBar = {
            if (showBottomBar && !useNavigationRail) {
                CustomBottomNavigation(
                    navController = navController,
                    currentDestination = currentDestination,
                    primaryItems = visiblePrimaryNavItems,
                    secondaryItems = visibleSecondaryNavItems,
                    onOpenMoreSheet = { showMoreSheet = true }
                )
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showBottomBar && useNavigationRail) {
                CustomNavigationRail(
                    navController = navController,
                    currentDestination = currentDestination,
                    primaryItems = visiblePrimaryNavItems,
                    secondaryItems = visibleSecondaryNavItems,
                    onOpenMoreSheet = { showMoreSheet = true }
                )
                VerticalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                enterTransition = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(initialOffsetX = { (it * 0.08f).toInt() }, animationSpec = tween(220, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    slideOutHorizontally(targetOffsetX = { -(it * 0.05f).toInt() }, animationSpec = tween(180, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(initialOffsetX = { -(it * 0.08f).toInt() }, animationSpec = tween(220, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    slideOutHorizontally(targetOffsetX = { (it * 0.05f).toInt() }, animationSpec = tween(180, easing = FastOutLinearInEasing))
                }
            ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        navController.navigate(Screen.OtpVerify.createRoute(userId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onAuthenticated = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onAuthenticated = { userCount, phone ->
                        navController.navigate(Screen.SubscriptionActivation.createRoute(userCount, phone)) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOtpRequired = { userId, userCount, phone ->
                        navController.navigate(Screen.OtpVerify.createRoute(userId, userCount, phone)) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.OtpVerify.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val userCount = backStackEntry.arguments?.getString("userCount")?.toIntOrNull() ?: 0
                val phone = android.net.Uri.decode(backStackEntry.arguments?.getString("phone").orEmpty())
                com.app.biashara.ui.screens.auth.OtpScreen(
                    userId = userId,
                    onVerified = {
                        val destination = if (userCount > 0) Screen.SubscriptionActivation.createRoute(userCount, phone) else Screen.Dashboard.route
                        navController.navigate(destination) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.SubscriptionActivation.route) { backStackEntry ->
                val userCount = backStackEntry.arguments?.getString("userCount")?.toIntOrNull() ?: 1
                val phone = android.net.Uri.decode(backStackEntry.arguments?.getString("phone").orEmpty())
                SubscriptionActivationScreen(
                    userCount = userCount,
                    phone = phone,
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            // Main screens
            composable(Screen.Dashboard.route) {
                NativeAccessGate(canOpen(Screen.Dashboard), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    DashboardScreen(
                        navController = navController,
                        onLogout = {
                            coroutineScope.launch {
                                authRepository.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
            composable(Screen.Pos.route) {
                NativeAccessGate(canOpen(Screen.Pos), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    PosScreen(windowWidthSizeClass = windowWidthSizeClass)
                }
            }
            composable(Screen.Inventory.route) {
                NativeAccessGate(canOpen(Screen.Inventory), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    InventoryScreen(
                        onAddProduct = { navController.navigate(Screen.AddProduct.createRoute()) },
                        onEditProduct = { id -> navController.navigate(Screen.AddProduct.createRoute(id)) }
                    )
                }
            }
            composable(Screen.Orders.route) {
                NativeAccessGate(canOpen(Screen.Orders), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    OrdersScreen(
                        onOrderDetail = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) },
                        onCreateOrder = { navController.navigate(Screen.CreateOrder.route) }
                    )
                }
            }
            composable(Screen.Customers.route) {
                NativeAccessGate(canOpen(Screen.Customers), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    CustomersScreen(
                        onCustomerDetail = { id -> navController.navigate(Screen.CustomerDetail.createRoute(id)) }
                    )
                }
            }
            composable(Screen.Expenses.route) {
                NativeAccessGate(canOpen(Screen.Expenses), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    ExpensesScreen(
                        onAddExpense = { navController.navigate(Screen.AddExpense.route) }
                    )
                }
            }
            composable(Screen.Payments.route) {
                NativeAccessGate(canOpen(Screen.Payments), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    PaymentsScreen()
                }
            }
            composable(Screen.PaymentConfiguration.route) {
                NativeAccessGate(canOpen(Screen.PaymentConfiguration), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.settings.PaymentConfigurationScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.Tax.route) {
                NativeAccessGate(canOpen(Screen.Tax), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    TaxScreen(onConfigureKra = { navController.navigate(Screen.Kra.route) })
                }
            }
            composable(Screen.Kra.route) {
                NativeAccessGate(canOpen(Screen.Kra), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {

                }
            }
            composable(Screen.Social.route) {
                NativeAccessGate(canOpen(Screen.Social), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {

                }
            }
            composable(Screen.HospitalityOperations.route) {
                NativeAccessGate(canOpen(Screen.HospitalityOperations), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    if (hospitalityEnabled) HospitalityOperationsScreen()
                    else LaunchedEffect(Unit) { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.HospitalityOperations.route) { inclusive = true } } }
                }
            }
            // Detail / sub-screens
            composable(Screen.AddProduct.route) { backStackEntry ->
                NativeAccessGate(canOpen(Screen.AddProduct, if (backStackEntry.arguments?.getString("productId").isNullOrBlank()) "products.create" else "products.update"), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    val productId = backStackEntry.arguments?.getString("productId")
                    com.app.biashara.ui.screens.inventory.AddProductScreen(
                        productId = productId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.OrderDetail.route) { backStackEntry ->
                NativeAccessGate(canOpen(Screen.OrderDetail), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                    com.app.biashara.ui.screens.orders.OrderDetailScreen(
                        orderId = orderId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.CreateOrder.route) {
                NativeAccessGate(canOpen(Screen.CreateOrder), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.orders.CreateOrderScreen(
                        onBack = { navController.popBackStack() },
                        onOrderCreated = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.CustomerDetail.route) { backStackEntry ->
                NativeAccessGate(canOpen(Screen.CustomerDetail), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                    com.app.biashara.ui.screens.customers.CustomerDetailScreen(
                        customerId = customerId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.AddExpense.route) {
                NativeAccessGate(canOpen(Screen.AddExpense), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.expenses.AddExpenseScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.Reports.route) {
                NativeAccessGate(canOpen(Screen.Reports), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.reports.ReportsScreen()
                }
            }
            composable(Screen.Settings.route) {
                NativeAccessGate(canOpen(Screen.Settings), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.settings.SettingsScreen(
                        onLogout = {
                            coroutineScope.launch {
                                authRepository.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToPayments = { navController.navigate(Screen.PaymentConfiguration.route) },
                        onNavigateToTax = { navController.navigate(Screen.Tax.route) },
                        onNavigateToKra = { navController.navigate(Screen.Kra.route) },
                        onNavigateToCyberSourceSettings = { navController.navigate(Screen.CyberSourceSettings.route) }
                    )
                }
            }
            composable(Screen.CyberSourceSettings.route) {
                NativeAccessGate(canOpen(Screen.CyberSourceSettings), accessLoading, accessError,
                    onRetry = { accessReload++ }, onBack = ::goBackFromAccessGate) {
                    com.app.biashara.ui.screens.settings.PaymentConfigurationScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
        }
    }
    }
}

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    currentDestination: NavDestination?,
    primaryItems: List<com.app.biashara.ui.navigation.BottomNavItem>,
    secondaryItems: List<com.app.biashara.ui.navigation.BottomNavItem>,
    onOpenMoreSheet: () -> Unit
) {
    val isAnySecondarySelected = secondaryItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Surface(
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) B360Green else Color.Transparent,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabPillColor"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF64748B),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabIconColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) B360Green else Color(0xFF64748B),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabTextColor"
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "tabPillScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            selected = isSelected
                            contentDescription = "${item.label} tab${if (isSelected) ", selected" else ""}"
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pillColor,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .graphicsLayer {
                                scaleX = pillScale
                                scaleY = pillScale
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val morePillColor by animateColorAsState(
                targetValue = if (isAnySecondarySelected) B360Green else Color.Transparent,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "morePillColor"
            )
            val moreIconColor by animateColorAsState(
                targetValue = if (isAnySecondarySelected) Color.White else Color(0xFF64748B),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "moreIconColor"
            )
            val moreTextColor by animateColorAsState(
                targetValue = if (isAnySecondarySelected) B360Green else Color(0xFF64748B),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "moreTextColor"
            )
            val morePillScale by animateFloatAsState(
                targetValue = if (isAnySecondarySelected) 1f else 0.94f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "morePillScale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        selected = isAnySecondarySelected
                        contentDescription = "More Apps & Operations tab${if (isAnySecondarySelected) ", selected" else ""}"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = onOpenMoreSheet
                    )
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = morePillColor,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .graphicsLayer {
                            scaleX = morePillScale
                            scaleY = morePillScale
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "More",
                            tint = moreIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "More",
                    color = moreTextColor,
                    fontSize = 11.sp,
                    fontWeight = if (isAnySecondarySelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CustomNavigationRail(
    navController: NavController,
    currentDestination: NavDestination?,
    primaryItems: List<com.app.biashara.ui.navigation.BottomNavItem>,
    secondaryItems: List<com.app.biashara.ui.navigation.BottomNavItem>,
    onOpenMoreSheet: () -> Unit
) {
    val isAnySecondarySelected = secondaryItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    NavigationRail(
        containerColor = Color.White,
        contentColor = Color(0xFF0F172A),
        header = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = B360Green.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "360",
                        color = B360Green,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            primaryItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = B360Green,
                        indicatorColor = B360Green,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }

            NavigationRailItem(
                selected = isAnySecondarySelected,
                onClick = onOpenMoreSheet,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = "More",
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = "More",
                        fontSize = 11.sp,
                        fontWeight = if (isAnySecondarySelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = B360Green,
                    indicatorColor = B360Green,
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAppsBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    secondaryItems: List<com.app.biashara.ui.navigation.BottomNavItem>
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apps & Operations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            val tileColors = mapOf(
                Screen.Customers.route to Pair(Color(0xFFEA580C), Color(0xFFFEF3C7)),
                Screen.Social.route to Pair(Color(0xFF9333EA), Color(0xFFF3E8FF)),
                Screen.HospitalityOperations.route to Pair(Color(0xFF00B074), Color(0xFFE6F4EA)),
                Screen.Reports.route to Pair(Color(0xFF0284C7), Color(0xFFE0F2FE)),
                Screen.Payments.route to Pair(Color(0xFF2563EB), Color(0xFFE8F0FE)),
                Screen.Settings.route to Pair(Color(0xFF475569), Color(0xFFF1F5F9))
            )

            val itemByRoute = secondaryItems.associateBy { it.screen.route }
            val groupedItems = listOf(
                "OPERATIONS" to listOf(Screen.HospitalityOperations.route, Screen.Customers.route),
                "FINANCE" to listOf(Screen.Payments.route),
                "ENGAGEMENT" to listOf(Screen.Social.route, Screen.Reports.route),
                "ADMINISTRATION" to listOf(Screen.Settings.route)
            )

            groupedItems.forEach { (groupLabel, routes) ->
                val groupItems = routes.mapNotNull { itemByRoute[it] }
                if (groupItems.isNotEmpty()) {
                    Text(
                        text = groupLabel,
                        modifier = Modifier.padding(top = 4.dp),
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    groupItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                val (iconColor, bgColor) = tileColors[item.screen.route]
                                    ?: Pair(Color(0xFF2563EB), Color(0xFFE8F0FE))
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(88.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            onDismiss()
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    color = bgColor,
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = iconColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iconColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
