package com.app.biashara.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.screens.*
import com.app.biashara.ui.theme.*

sealed class DesktopScreen(val label: String, val icon: ImageVector) {
    object Dashboard : DesktopScreen("Dashboard", Icons.Filled.Home)
    object Inventory : DesktopScreen("Inventory", Icons.Filled.Inventory)
    object Orders : DesktopScreen("Orders", Icons.Filled.ShoppingCart)
    object Customers : DesktopScreen("Customers", Icons.Filled.People)
    object Expenses : DesktopScreen("Expenses", Icons.Filled.Receipt)
    object Payments : DesktopScreen("Payments", Icons.Filled.AccountBalance)
    object Tax : DesktopScreen("Tax", Icons.Filled.Percent)
    object Kra : DesktopScreen("KRA iTax", Icons.Filled.Assignment)
    object Social : DesktopScreen("Social Inbox", Icons.Filled.Forum)
    object Reports : DesktopScreen("Reports", Icons.Filled.BarChart)
    object Settings : DesktopScreen("Settings", Icons.Filled.Settings)
}

val navItems = listOf(
    DesktopScreen.Dashboard,
    DesktopScreen.Inventory,
    DesktopScreen.Orders,
    DesktopScreen.Customers,
    DesktopScreen.Expenses,
    DesktopScreen.Payments,
    DesktopScreen.Tax,
    DesktopScreen.Kra,
    DesktopScreen.Social,
    DesktopScreen.Reports
)

@Composable
fun Biashara360DesktopApp() {
    Biashara360DesktopTheme {
        var currentScreen by remember { mutableStateOf<DesktopScreen>(DesktopScreen.Dashboard) }

        Row(Modifier.fillMaxSize()) {
            // ── Sidebar ──────────────────────────────────────────────────────
            DesktopSidebar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )

            // ── Main Content ─────────────────────────────────────────────────
            Column(
                Modifier.fillMaxSize().background(B360Surface)
            ) {
                // Top bar
                DesktopTopBar(currentScreen)
                // Screen content
                AnimatedContent(targetState = currentScreen) { screen ->
                    when (screen) {
                        is DesktopScreen.Dashboard -> DesktopDashboardScreen()
                        is DesktopScreen.Inventory -> DesktopInventoryScreen()
                        is DesktopScreen.Orders -> DesktopOrdersScreen()
                        is DesktopScreen.Customers -> DesktopCustomersScreen()
                        is DesktopScreen.Expenses -> DesktopExpensesScreen()
                        is DesktopScreen.Payments -> DesktopPaymentsScreen()
                        is DesktopScreen.Tax -> DesktopTaxScreen()
                        is DesktopScreen.Kra -> DesktopKraScreen()
                        is DesktopScreen.Social -> DesktopSocialScreen()
                        is DesktopScreen.Reports -> DesktopReportsScreen()
                        is DesktopScreen.Settings -> DesktopSettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopSidebar(currentScreen: DesktopScreen, onScreenSelected: (DesktopScreen) -> Unit) {
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(B360SidebarBg)
            .padding(vertical = 16.dp)
    ) {
        // Logo
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Biashara360ERP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Business Management", color = Color.White.copy(0.5f), fontSize = 11.sp)
        }

        Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 8.dp))

        // Nav items
        navItems.forEach { screen ->
            SidebarItem(
                screen = screen,
                isSelected = currentScreen == screen,
                onClick = { onScreenSelected(screen) }
            )
        }

        Spacer(Modifier.weight(1f))
        Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 8.dp))

        // Settings
        SidebarItem(
            screen = DesktopScreen.Settings,
            isSelected = currentScreen == DesktopScreen.Settings,
            onClick = { onScreenSelected(DesktopScreen.Settings) }
        )

        // User profile
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(B360Green),
                contentAlignment = Alignment.Center
            ) {
                Text("W", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Wanjiru Kamau", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Admin", color = Color.White.copy(0.5f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SidebarItem(screen: DesktopScreen, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) B360SidebarSelected else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.White.copy(0.6f)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isSelected) {
            Box(Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(B360Green))
        } else {
            Spacer(Modifier.width(3.dp))
        }
        Icon(screen.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(screen.label, color = contentColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun DesktopTopBar(currentScreen: DesktopScreen) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(currentScreen.label, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.width(280.dp).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
            IconButton(onClick = {}) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(Icons.Filled.Notifications, null)
                }
            }
        }
    }
    Divider(color = Color(0xFFF0F0F0))
}
