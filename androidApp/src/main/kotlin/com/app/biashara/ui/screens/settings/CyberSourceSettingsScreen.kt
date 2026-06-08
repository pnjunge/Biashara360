package com.app.biashara.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberSourceSettingsScreen(
    onBack: () -> Unit
) {
    var merchantId by remember { mutableStateOf("WanFashion_CS_098") }
    var merchantKeyId by remember { mutableStateOf("9c7c25eb-42f8-4a52-b8bb-69d2d0c2e39b") }
    var merchantSecretKey by remember { mutableStateOf("••••••••••••••••••••••••••••••••") }
    var isSandbox by remember { mutableStateOf(true) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CyberSource Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("API Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = B360Green)
                    HorizontalDivider(color = Color(0xFFF5F5F5))

                    // Merchant ID
                    OutlinedTextField(
                        value = merchantId,
                        onValueChange = { merchantId = it; showSuccessMessage = false },
                        label = { Text("Merchant ID (Organization ID)") },
                        placeholder = { Text("e.g. wanfashion_cs_098") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Key ID
                    OutlinedTextField(
                        value = merchantKeyId,
                        onValueChange = { merchantKeyId = it; showSuccessMessage = false },
                        label = { Text("Active Key ID (JWT/P12 Key ID)") },
                        placeholder = { Text("e.g. 9c7c25eb-xxxx-xxxx-xxxx-xxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Secret Key
                    OutlinedTextField(
                        value = merchantSecretKey,
                        onValueChange = { merchantSecretKey = it; showSuccessMessage = false },
                        label = { Text("Shared Secret Key") },
                        placeholder = { Text("Enter secure shared secret key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Environment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Sandbox Environment", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Toggle off for production rails", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isSandbox,
                            onCheckedChange = { isSandbox = it; showSuccessMessage = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = B360Green
                            )
                        )
                    }

                    if (showSuccessMessage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(B360Green.copy(0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("✓ Configuration saved and validated successfully!", color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = { showSuccessMessage = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
