package com.app.biashara.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable
fun Biashara360App() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        navController.navigate(Screen.OtpVerify.createRoute(userId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
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
                    }
                )
            }
        }
    }
}
