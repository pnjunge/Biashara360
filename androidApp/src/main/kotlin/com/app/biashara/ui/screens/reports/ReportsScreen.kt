package com.app.biashara.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("Today", "This Week", "This Month", "This Quarter", "This Year")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reports", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Period selector
                ScrollableTabRow(selectedTabIndex = periods.indexOf(selectedPeriod), containerColor = Color.Transparent, edgePadding = 0.dp) {
                    periods.forEachIndexed { i, period ->
                        Tab(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            text = { Text(period, fontSize = 13.sp) }
                        )
                    }
                }
            }
            item {
                // P&L Summary
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Profit & Loss", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        PnlRow("Revenue", "KES 320,000", B360Green)
                        PnlRow("Cost of Goods", "KES 180,000", Color(0xFFC62828))
                        Divider()
                        PnlRow("Gross Profit", "KES 140,000", B360Green, bold = true)
                        PnlRow("Expenses", "KES 45,000", Color(0xFFC62828))
                        Divider()
                        PnlRow("Net Profit", "KES 95,000", B360Green, bold = true)
                    }
                }
            }
            item {
                // KPI cards
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Orders" to "127", "Customers" to "48", "Avg Order" to "KES 2,520").forEachIndexed { i, (label, value) ->
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = B360Green)
                                Text(label, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            item {
                // Top products
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Top Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        listOf(
                            Triple("Nike Air Max", "34 sold", "KES 102,000"),
                            Triple("Adidas Hoodie", "22 sold", "KES 66,000"),
                            Triple("Levi's Jeans", "18 sold", "KES 54,000"),
                        ).forEach { (name, qty, revenue) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column { Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(qty, fontSize = 12.sp, color = Color.Gray) }
                                Text(revenue, fontSize = 14.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PnlRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
