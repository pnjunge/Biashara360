package com.app.biashara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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

@Composable
fun Biashara360App() {
    val startDestination = Screen.Login.route
    val tokenStorage = koinInject<TokenStorage>()
    val authRepository = koinInject<AuthRepository>()
    val client = koinInject<HttpClient>()
    val coroutineScope = rememberCoroutineScope()
    var sessionWarningSeconds by remember { mutableStateOf<Int?>(null) }
    val networkAvailable = rememberNetworkAvailable()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var hospitalityEnabled by remember { mutableStateOf(false) }
    var enabledMenus by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(currentDestination?.route) {
        if (currentDestination?.route !in setOf(Screen.Login.route, Screen.Register.route, Screen.OtpVerify.route)) {
            runCatching { client.get("$BASE_URL/hospitality/status").body<ApiResponse<HospitalityStatus>>() }
                .onSuccess { hospitalityEnabled = it.success && it.data?.enabled == true }
            runCatching { client.get("$BASE_URL/access/me").body<ApiResponse<MenuAccess>>() }
                .onSuccess { if (it.success) enabledMenus = it.data?.enabledMenus?.toSet() }
        }
    }

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

    fun isMenuEnabled(item: com.app.biashara.ui.navigation.BottomNavItem): Boolean {
        if (enabledMenus == null) {
            return item.screen != Screen.HospitalityOperations || hospitalityEnabled
        }
        return when (item.screen) {
            Screen.Dashboard -> enabledMenus?.contains("DASHBOARD") == true
            Screen.Pos -> enabledMenus?.contains("POS") == true
            Screen.Orders -> enabledMenus?.contains("ORDERS") == true
            Screen.Inventory -> enabledMenus?.contains("INVENTORY") == true
            Screen.Customers -> enabledMenus?.contains("CUSTOMERS") == true
            Screen.Social -> enabledMenus?.contains("SOCIAL") == true || enabledMenus?.contains("SOCIAL_SETUP") == true
            Screen.HospitalityOperations -> (enabledMenus?.contains("HOSPITALITY_OPS") == true || enabledMenus?.contains("HOSPITALITY") == true) && hospitalityEnabled
            Screen.Reports -> enabledMenus?.contains("REPORTS") == true
            Screen.Payments -> enabledMenus?.contains("PAYMENTS") == true || enabledMenus?.contains("CARD_PAYMENTS") == true
            Screen.Tax -> enabledMenus?.contains("TAX") == true || enabledMenus?.contains("TAX_COMPLIANCE") == true
            Screen.Kra -> enabledMenus?.contains("KRA") == true || enabledMenus?.contains("TAX_COMPLIANCE") == true
            Screen.Settings -> enabledMenus?.contains("SETTINGS") == true
            else -> true
        }
    }

    val visiblePrimaryNavItems = primaryBottomNavItems.filter { isMenuEnabled(it) }
    val visibleSecondaryNavItems = secondaryNavItems.filter { isMenuEnabled(it) }

    val showBottomBar = (visiblePrimaryNavItems + visibleSecondaryNavItems).any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    if (showMoreSheet) {
        MoreAppsBottomSheet(
            onDismiss = { showMoreSheet = false },
            navController = navController,
            secondaryItems = visibleSecondaryNavItems
        )
    }

    CompositionLocalProvider(LocalNetworkAvailable provides networkAvailable) {
    Scaffold(
        topBar = {
            if (!networkAvailable) {
                Surface(color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFF92400E))
                        Text(
                            "Offline — showing cached data. Saving and payments are unavailable.",
                            color = Color(0xFF92400E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
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
                    onRegistered = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.OtpVerify.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                com.app.biashara.ui.screens.auth.OtpScreen(
                    userId = userId,
                    onVerified = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            // Main screens
            composable(Screen.Dashboard.route) {
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
            composable(Screen.Pos.route) {
                PosScreen()
            }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    onAddProduct = { navController.navigate(Screen.AddProduct.createRoute()) },
                    onEditProduct = { id -> navController.navigate(Screen.AddProduct.createRoute(id)) }
                )
            }
            composable(Screen.Orders.route) {
                OrdersScreen(
                    onOrderDetail = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) },
                    onCreateOrder = { navController.navigate(Screen.CreateOrder.route) }
                )
            }
            composable(Screen.Customers.route) {
                CustomersScreen(
                    onCustomerDetail = { id -> navController.navigate(Screen.CustomerDetail.createRoute(id)) }
                )
            }
            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    onAddExpense = { navController.navigate(Screen.AddExpense.route) }
                )
            }
            composable(Screen.Payments.route) {
                PaymentsScreen()
            }
            composable(Screen.Tax.route) {
                TaxScreen(onConfigureKra = { navController.navigate(Screen.Kra.route) })
            }
            composable(Screen.Kra.route) { KraScreen() }
            composable(Screen.Social.route) { SocialScreen() }
            composable(Screen.HospitalityOperations.route) {
                if (hospitalityEnabled) HospitalityOperationsScreen()
                else LaunchedEffect(Unit) { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.HospitalityOperations.route) { inclusive = true } } }
            }
            // Detail / sub-screens
            composable(Screen.AddProduct.route) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                com.app.biashara.ui.screens.inventory.AddProductScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.OrderDetail.route) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                com.app.biashara.ui.screens.orders.OrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CreateOrder.route) {
                com.app.biashara.ui.screens.orders.CreateOrderScreen(
                    onBack = { navController.popBackStack() },
                    onOrderCreated = { navController.popBackStack() }
                )
            }
            composable(Screen.CustomerDetail.route) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                com.app.biashara.ui.screens.customers.CustomerDetailScreen(
                    customerId = customerId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddExpense.route) {
                com.app.biashara.ui.screens.expenses.AddExpenseScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) {
                com.app.biashara.ui.screens.reports.ReportsScreen()
            }
            composable(Screen.Settings.route) {
                com.app.biashara.ui.screens.settings.SettingsScreen(
                    onLogout = {
                        coroutineScope.launch {
                            authRepository.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                    onNavigateToTax = { navController.navigate(Screen.Tax.route) },
                    onNavigateToKra = { navController.navigate(Screen.Kra.route) },
                    onNavigateToCyberSourceSettings = { navController.navigate(Screen.CyberSourceSettings.route) }
                )
            }
            composable(Screen.CyberSourceSettings.route) {
                com.app.biashara.ui.screens.settings.PaymentConfigurationScreen(
                    onBack = { navController.popBackStack() }
                )
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
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = B360Green,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            color = B360Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Box(
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

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
                if (isAnySecondarySelected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = B360Green,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "More",
                        color = B360Green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Box(
                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "More",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "More",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
