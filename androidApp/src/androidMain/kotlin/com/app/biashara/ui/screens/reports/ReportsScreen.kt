package com.app.biashara.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.app.biashara.presentation.viewmodel.ReportsViewModel
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = kmpViewModel()) {
    val state by viewModel.state.collectAsState()
    val periods = listOf("Today", "This Week", "This Month", "This Quarter", "This Year")
    val selectedPeriod = state.selectedPeriodLabel

    LaunchedEffect(Unit) { viewModel.loadReport("This Month") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports / Ripoti", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(B360Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScrollableTabRow(
                    selectedTabIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[periods.indexOf(selectedPeriod).coerceAtLeast(0)]),
                            color = B360Green
                        )
                    }
                ) {
                    periods.forEachIndexed { i, period ->
                        val isSelected = selectedPeriod == period
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.loadReport(period) },
                            text = { Text(period, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) B360Green else Color(0xFF64748B)) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = B360Green)
                    }
                }
                return@LazyColumn
            }

            if (state.error != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.2f)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                return@LazyColumn
            }

            val summary = state.profitSummary
            if (summary != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Profit & Loss", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            PnlRow("Revenue", "KES ${"%,.0f".format(summary.totalRevenue)}", B360Green)
                            PnlRow("Cost of Goods", "KES ${"%,.0f".format(summary.totalCostOfGoods)}", Color(0xFF64748B))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            PnlRow("Gross Profit", "KES ${"%,.0f".format(summary.grossProfit)}", if (summary.grossProfit >= 0) B360Green else B360Red, bold = true)
                            PnlRow("Total Expenses", "KES ${"%,.0f".format(summary.totalExpenses)}", B360Red)
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            PnlRow("Net Profit", "KES ${"%,.0f".format(summary.netProfit)}",
                                if (summary.netProfit >= 0) B360Green else B360Red, bold = true, large = true)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard(Modifier.weight(1f), "Gross Margin", "${"%,.1f".format(summary.grossMargin)}%", B360Blue)
                        KpiCard(Modifier.weight(1f), "Net Margin", "${"%,.1f".format(summary.netMargin)}%",
                            if (summary.netMargin >= 0) B360Green else B360Red)
                    }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cash Flow", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            PnlRow("Cash In (Payments)", "KES ${"%,.0f".format(summary.cashflowIn)}", B360Green)
                            PnlRow("Cash Out (Ops + Expenses)", "KES ${"%,.0f".format(summary.cashflowOut)}", B360Red)
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            PnlRow("Net Cash Flow", "KES ${"%,.0f".format(summary.netCashflow)}",
                                if (summary.netCashflow >= 0) B360Green else B360Red, bold = true)
                        }
                    }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.BarChart, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text("No data for $selectedPeriod", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PnlRow(label: String, value: String, valueColor: Color, bold: Boolean = false, large: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (bold) Color(0xFF0F172A) else Color(0xFF64748B), fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (large) 18.sp else 14.sp)
    }
}

@Composable
fun KpiCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 20.sp)
        }
    }
}
