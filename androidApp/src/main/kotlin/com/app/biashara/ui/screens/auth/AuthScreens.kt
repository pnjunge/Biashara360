package com.app.biashara.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green
import com.app.biashara.ui.theme.B360GreenDark

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(B360Green, B360GreenDark)))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Biashara360", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Biashara yako, nguvu yako", color = Color.White.copy(0.8f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your business, your power", color = Color.White.copy(0.6f), fontSize = 12.sp)
        Spacer(Modifier.height(40.dp))

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Welcome Back", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email / Phone") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton({ passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        isLoading = true
                        // Simulate login → normally calls AuthRepository
                        onLoginSuccess("user-123")
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Text("Login / Ingia", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't have an account? Register", textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("RETAIL") }

    val businessTypes = listOf("RETAIL" to "Retail Seller", "SERVICE" to "Service Provider", "HYBRID" to "Hybrid Business", "ONLINE_SELLER" to "Online Seller")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = B360Green)
        Text("Jiunge na Biashara360", color = Color.Gray)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (07XX)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), singleLine = true)

        Text("Business Type", fontWeight = FontWeight.Medium)
        businessTypes.forEach { (value, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = selectedType == value, onClick = { selectedType = value }, colors = RadioButtonDefaults.colors(selectedColor = B360Green))
                Text(label)
            }
        }

        Button(
            onClick = onRegistered,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Register / Jisajili", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun OtpScreen(userId: String, onVerified: () -> Unit) {
    var otp by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("SMS") }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Filled.Security, null, tint = B360Green, modifier = Modifier.size(64.dp))
        Text("Two-Factor Authentication", fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
        Text("Enter the OTP sent to your phone/email", color = Color.Gray, textAlign = TextAlign.Center)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("SMS", "Email", "App").forEach { ch ->
                FilterChip(
                    selected = selectedChannel == ch,
                    onClick = { selectedChannel = ch },
                    label = { Text(ch) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = B360Green.copy(0.2f))
                )
            }
        }

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it },
            label = { Text("Enter 6-digit OTP") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Button(
            onClick = onVerified,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
            shape = RoundedCornerShape(12.dp),
            enabled = otp.length == 6
        ) { Text("Verify / Thibitisha", fontWeight = FontWeight.Bold) }

        TextButton(onClick = {}) { Text("Resend OTP") }
    }
}
