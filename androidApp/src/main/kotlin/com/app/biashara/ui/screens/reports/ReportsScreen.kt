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
import com.app.biashara.presentation.viewmodel.ReportsViewModel
import com.app.biashara.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    val periods = listOf("Today", "This Week", "This Month", "This Quarter", "This Year")
    val selectedPeriod = state.selectedPeriodLabel

    LaunchedEffect(Unit) { viewModel.loadReport("This Month") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reports / Ripoti", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScrollableTabRow(
                    selectedTabIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp
                ) {
                    periods.forEachIndexed { i, period ->
                        Tab(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.loadReport(period) },
                            text = { Text(period, fontSize = 13.sp) }
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
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                return@LazyColumn
            }

            val summary = state.profitSummary
            if (summary != null) {
                item {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Profit & Loss", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            PnlRow("Revenue", "KES ${"%,.0f".format(summary.totalRevenue)}", B360Green)
                            PnlRow("Cost of Goods", "KES ${"%,.0f".format(summary.totalCostOfGoods)}", Color.Gray)
                            Divider()
                            PnlRow("Gross Profit", "KES ${"%,.0f".format(summary.grossProfit)}", if (summary.grossProfit >= 0) B360Green else B360Red, bold = true)
                            PnlRow("Total Expenses", "KES ${"%,.0f".format(summary.totalExpenses)}", B360Red)
                            Divider()
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
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cash Flow", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            PnlRow("Cash In (Payments)", "KES ${"%,.0f".format(summary.cashflowIn)}", B360Green)
                            PnlRow("Cash Out (Ops + Expenses)", "KES ${"%,.0f".format(summary.cashflowOut)}", B360Red)
                            Divider()
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
        Text(label, color = if (bold) MaterialTheme.colorScheme.onSurface else Color.Gray, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (large) 18.sp else 14.sp)
    }
}

@Composable
fun KpiCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(color.copy(0.08f))) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = color.copy(0.8f))
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp)
        }
    }
}
