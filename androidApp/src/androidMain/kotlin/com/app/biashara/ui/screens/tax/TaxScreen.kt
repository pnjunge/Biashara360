package com.app.biashara.ui.screens.tax

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxScreen(onConfigureKra: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Rates", "Calculator", "Remittances")

    Scaffold(
        containerColor = B360Surface,
        topBar = {
            TopAppBar(
                title = { Text("Tax Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = B360Green,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.White) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }
            when (tab) {
                0 -> TaxUnavailable(
                    "Tax summary unavailable",
                    "Connect live tax reporting before using this screen. No estimated liabilities or deadlines are displayed."
                )
                1 -> TaxUnavailable(
                    "Tax rates unavailable",
                    "Rates must come from your saved business tax configuration. Manage tax configuration in the web application."
                )
                2 -> TaxEstimator()
                else -> TaxUnavailable(
                    "No remittance data",
                    "Configure KRA to retrieve filed and paid remittances.",
                    "Configure KRA",
                    onConfigureKra
                )
            }
        }
    }
}

@Composable
private fun TaxEstimator() {
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    val amountValue = amount.toDoubleOrNull()
    val rateValue = rate.toDoubleOrNull()
    val tax = if (amountValue != null && rateValue != null && amountValue >= 0 && rateValue >= 0) {
        amountValue * rateValue / 100
    } else {
        null
    }

    Column(
        modifier = Modifier.fillMaxSize().background(B360Surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Manual tax estimator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Enter the applicable rate yourself. This estimate is not a filed return or KRA liability.",
            color = Color(0xFF64748B),
            fontSize = 13.sp
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Taxable amount (KES)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Tax rate (%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = B360Green)
                    Text("Estimated tax", fontWeight = FontWeight.Bold)
                }
                Text(
                    tax?.let { "KES ${"%,.2f".format(it)}" } ?: "—",
                    color = B360Green,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TaxUnavailable(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(B360Surface).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = B360Green)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(message, color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                actionLabel?.let {
                    Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text(it)
                    }
                }
            }
        }
    }
}
