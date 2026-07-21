package com.app.biashara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.domain.repository.AuthRepository
import com.app.biashara.ui.theme.B360Green
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.biashara.ui.navigation.Screen
import com.app.biashara.ui.navigation.bottomNavItems
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
import org.koin.compose.koinInject

@Composable
fun Biashara360App() {
    val authRepository = koinInject<AuthRepository>()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authRepository) {
        startDestination = if (authRepository.loginWithBiometric().isSuccess) {
            Screen.Dashboard.route
        } else {
            Screen.Login.route
        }
    }

    // Avoid briefly presenting the login screen while an encrypted saved session is restored.
    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CustomBottomNavigation(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
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
                DashboardScreen(navController = navController)
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
                TaxScreen()
            }
            composable(Screen.Kra.route) { KraScreen() }
            composable(Screen.Social.route) { SocialScreen() }
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
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                    onNavigateToKra = { navController.navigate(Screen.Kra.route) },
                    onNavigateToSocial = { navController.navigate(Screen.Social.route) },
                    onNavigateToCyberSourceSettings = { navController.navigate(Screen.CyberSourceSettings.route) }
                )
            }
            composable(Screen.CyberSourceSettings.route) {
                com.app.biashara.ui.screens.settings.CyberSourceSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    currentDestination: NavDestination?
) {
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
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
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
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            color = B360Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
