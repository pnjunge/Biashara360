package com.app.biashara.ui.screens.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen() {
    data class PaymentUi(val txCode: String, val name: String, val phone: String, val amount: Double, val channel: String, val date: String, val reconciled: Boolean)

    val payments = listOf(
        PaymentUi("RGK71HXYZ", "Amina Hassan", "0712345678", 4500.0, "Mpesa", "Today 2:30PM", true),
        PaymentUi("PLM23NQRS", "David Kamau", "0745678901", 6800.0, "Mpesa", "Yesterday 4:15PM", true),
        PaymentUi("QWE45RTYU", "Sarah Wangui", "0767890123", 1200.0, "Airtel Money", "Yesterday 1:00PM", false),
        PaymentUi("ZXC89VBNM", "Tom Mutua", "0778901234", 2300.0, "Mpesa", "Mon 3:45PM", false)
    )

    val totalCollected = payments.filter { it.reconciled }.sumOf { it.amount }
    val pendingReconciliation = payments.filter { !it.reconciled }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments / Malipo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentStat(Modifier.weight(1f), "Reconciled", "KES ${"%,.0f".format(totalCollected)}", B360Green)
                    PaymentStat(Modifier.weight(1f), "Unmatched", "KES ${"%,.0f".format(pendingReconciliation)}", B360Amber)
                }
            }
            item {
                Text("Transaction History", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(payments) { payment ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(Color.White)
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(payment.txCode, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = B360Green)
                                if (payment.reconciled) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = B360Green, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Filled.PendingActions, null, tint = B360Amber, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(payment.name, fontWeight = FontWeight.Medium)
                            Text("${payment.phone} • ${payment.channel}", fontSize = 12.sp, color = Color.Gray)
                            Text(payment.date, fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("KES ${"%,.0f".format(payment.amount)}", fontWeight = FontWeight.Bold, color = B360Green)
                            if (!payment.reconciled) {
                                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                    Text("Match Order", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentStat(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(color.copy(0.1f))) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = color.copy(0.8f))
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
        }
    }
}
