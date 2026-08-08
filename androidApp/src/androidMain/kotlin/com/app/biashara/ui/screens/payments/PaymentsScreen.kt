package com.app.biashara.ui.screens.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.app.biashara.domain.model.Payment
import com.app.biashara.presentation.viewmodel.PaymentsViewModel
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel = kmpViewModel()) {
    val state by viewModel.state.collectAsState()
    var matchPayment by remember { mutableStateOf<Payment?>(null) }
    var matchOrderId by remember { mutableStateOf("") }
    var matchError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadPayments() }

    val reconcileResult by viewModel.reconcileResult.collectAsState(initial = null)
    LaunchedEffect(reconcileResult) {
        reconcileResult?.let {
            if (it.isSuccess) {
                matchPayment = null
                matchOrderId = ""
                matchError = ""
            } else {
                matchError = it.exceptionOrNull()?.message ?: "Failed to reconcile"
            }
        }
    }

    if (matchPayment != null) {
        AlertDialog(
            onDismissRequest = { matchPayment = null; matchError = "" },
            title = { Text("Match Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Matching: ${matchPayment!!.transactionCode}", fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Text("Amount: KES ${"%,.0f".format(matchPayment!!.amount)}", color = B360Green, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = matchOrderId,
                        onValueChange = { matchOrderId = it; matchError = "" },
                        label = { Text("Order Number / Order ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    if (matchError.isNotBlank()) {
                        Text(matchError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (matchOrderId.isBlank()) {
                            matchError = "Please enter an order number or ID"
                        } else {
                            viewModel.reconcilePayment(matchPayment!!.id, matchOrderId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("Match", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { matchPayment = null; matchError = "" }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments / Malipo", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        if (state.isLoading && state.payments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).background(B360Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentStat(Modifier.weight(1f), "Reconciled", "KES ${"%,.0f".format(state.totalReconciled)}", B360Green)
                    PaymentStat(Modifier.weight(1f), "Unmatched", "KES ${"%,.0f".format(state.totalUnmatched)}", B360Amber)
                }
            }
            item {
                Text("Transaction History", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF0F172A))
            }

            if (state.payments.isEmpty() && !state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Payments, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text("No payments recorded yet", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(state.payments) { payment ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(payment.transactionCode, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = B360Green)
                                    Icon(
                                        if (payment.reconciled) Icons.Filled.CheckCircle else Icons.Filled.PendingActions,
                                        null,
                                        tint = if (payment.reconciled) B360Green else B360Amber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(payment.payerName, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                                Text("${payment.payerPhone} • ${payment.method.name}", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(payment.transactionDate.toString().substring(0, 16).replace("T", " "), fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("KES ${"%,.0f".format(payment.amount)}", fontWeight = FontWeight.ExtraBold, color = B360Green, fontSize = 15.sp)
                                if (!payment.reconciled) {
                                    TextButton(
                                        onClick = { matchPayment = payment },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = B360Green)
                                    ) {
                                        Text("Match Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("✓ Matched", fontSize = 11.sp, color = B360Green, fontWeight = FontWeight.Bold)
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 16.sp)
        }
    }
}
