package com.app.biashara.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.ui.kmpViewModel
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberSourceSettingsScreen(
    onBack: () -> Unit,
    viewModel: BusinessViewModel = kmpViewModel()
) {
    val cyberSource by viewModel.cyberSourceState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCyberSourceConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CyberSource Settings", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCyberSourceConfig() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(B360Surface)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Secure Acceptance Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (cyberSource.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = B360Green)
                        }
                    } else {
                        val config = cyberSource.config

                        OutlinedTextField(
                            value = config?.merchantId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Merchant ID (Organization ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = config?.merchantKeyId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Merchant Key ID (REST API Key ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = config?.profileId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Secure Acceptance Profile ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = config?.accessKey ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Secure Acceptance Access Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = if (config?.secretConfigured == true) "•••••••• (Configured on backend)" else "Not configured",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Shared Secret Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Environment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text(if (config?.environment == "production") "Production Rails" else "Sandbox Rail", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text(
                                text = (config?.environment ?: "sandbox").uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (config?.environment == "production") B360Green else Color(0xFFD97706),
                                fontSize = 13.sp
                            )
                        }

                        cyberSource.error?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(B360Green.copy(0.12f), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("🔐 Payment credentials are synchronized from backend. Admin updates are managed securely on the Biashara360 web portal.", color = Color(0xFF166534), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

