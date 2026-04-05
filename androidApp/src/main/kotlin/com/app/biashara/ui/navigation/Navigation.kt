package com.app.biashara.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")
    object OtpVerify : Screen("otp_verify/{userId}") {
        fun createRoute(userId: String) = "otp_verify/$userId"
    }

    // Main
    object Dashboard : Screen("dashboard")
    object Inventory : Screen("inventory")
    object AddProduct : Screen("add_product?productId={productId}") {
        fun createRoute(productId: String? = null) =
            if (productId != null) "add_product?productId=$productId" else "add_product"
    }
    object Orders : Screen("orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object CreateOrder : Screen("create_order")
    object Customers : Screen("customers")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object Expenses : Screen("expenses")
    object AddExpense : Screen("add_expense")
    object Payments : Screen("payments")
    object Tax : Screen("tax")
    object Kra    : Screen("kra")
    object Social : Screen("social")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val labelSw: String   // Swahili label
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.Home, "Nyumbani"),
    BottomNavItem(Screen.Orders, "Orders", Icons.Filled.ShoppingCart, "Maagizo"),
    BottomNavItem(Screen.Inventory, "Stock", Icons.Filled.Inventory, "Hifadhi"),
    BottomNavItem(Screen.Customers, "Customers", Icons.Filled.People, "Wateja"),
    BottomNavItem(Screen.Expenses, "Expenses", Icons.Filled.AccountBalance, "Gharama")
)
