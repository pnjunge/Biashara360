package com.app.biashara.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.ui.kmpViewModel
import com.app.biashara.ui.theme.B360Green
import com.app.biashara.ui.theme.B360Surface
import com.app.biashara.ui.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfigurationScreen(
    onBack: () -> Unit,
    viewModel: BusinessViewModel = kmpViewModel()
) {
    SecureScreen()
    val mpesa by viewModel.mpesaState.collectAsState()
    val cyberSource by viewModel.cyberSourceState.collectAsState()

    fun refresh() {
        viewModel.loadMpesaConfig()
        viewModel.loadCyberSourceConfig()
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = B360Surface,
        topBar = {
            TopAppBar(
                title = { Text("Payment configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::refresh) {
                        Icon(Icons.Default.Refresh, "Refresh configuration")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "These settings are synchronized from the web application and are read-only on Android.",
                    color = Color(0xFF64748B)
                )
            }
            if (mpesa.configs.isEmpty()) {
                item {
                    MobileConfigCard("M-Pesa") {
                        MobileConfigRow("Status", "Not configured")
                        mpesa.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            } else {
                items(mpesa.configs, key = { it.accountType }) { config ->
                    MobileConfigCard("M-Pesa ${config.accountType.displayAccountType()}") {
                    MobileConfigRow("Status", "Configured")
                    MobileConfigRow("Shortcode", config.shortCode)
                    MobileConfigRow("Account type", config.accountType.displayAccountType())
                    MobileConfigRow("Environment", config.environment)
                    MobileConfigRow("Passkey", if (config.passkeyConfigured) "Configured" else "Not configured")
                    mpesa.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                MobileConfigCard("CyberSource Secure Acceptance") {
                    val config = cyberSource.config
                    MobileConfigRow("Status", if (config == null) "Not configured" else "Configured")
                    MobileConfigRow("Merchant ID", config?.merchantId ?: "—")
                    MobileConfigRow("Profile ID", config?.profileId ?: "—")
                    MobileConfigRow("Access Key", config?.accessKey ?: "—")
                    MobileConfigRow("Environment", config?.environment ?: "—")
                    MobileConfigRow("Shared secret", if (config?.secretConfigured == true) "Configured" else "Not configured")
                    cyberSource.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item {
                Surface(color = B360Green.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "Use the Biashara360 web application as a merchant admin to change payment credentials.",
                        Modifier.padding(16.dp),
                        color = Color(0xFF166534)
                    )
                }
            }
        }
    }
}

private fun String.displayAccountType(): String =
    lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun MobileConfigCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun MobileConfigRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF64748B))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
