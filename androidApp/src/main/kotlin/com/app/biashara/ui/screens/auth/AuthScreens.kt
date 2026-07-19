package com.app.biashara.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.domain.model.BusinessType
import com.app.biashara.presentation.viewmodel.AuthStep
import com.app.biashara.presentation.viewmodel.AuthViewModel
import com.app.biashara.ui.theme.B360Green
import com.app.biashara.ui.theme.B360GreenDark
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK

@Composable
fun AuthBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF015A42)) // Deep green brand background
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left soft arc
            drawCircle(
                color = Color(0xFF059669).copy(alpha = 0.25f),
                radius = width * 0.5f,
                center = androidx.compose.ui.geometry.Offset(-width * 0.1f, height * 0.1f)
            )

            // Outer top-left thin arc border
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.15f),
                radius = width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(-width * 0.1f, height * 0.1f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Bottom-left soft wave/arc
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.15f),
                radius = width * 0.4f,
                center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.95f)
            )
            
            // Bottom-left inner soft white/light-green wave
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.98f)
            )

            // Bottom-right soft arc
            drawCircle(
                color = Color(0xFF059669).copy(alpha = 0.2f),
                radius = width * 0.45f,
                center = androidx.compose.ui.geometry.Offset(width * 1.1f, height * 0.85f)
            )

            // Top-right dot grid
            val dotSpacing = 20.dp.toPx()
            val dotRadius = 2.dp.toPx()
            val startX = width * 0.7f
            val startY = height * 0.12f
            for (col in 0..6) {
                for (row in 0..9) {
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.2f),
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(startX + col * dotSpacing, startY + row * dotSpacing)
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun LogoHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, Color(0xFF34D399), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "B",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Biashara360",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Biashara yako, nguvu yako",
            color = Color(0xFF34D399),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Your business, your power",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun FloatingShieldBadge() {
    Box(
        modifier = Modifier
            .offset(y = (-32).dp)
            .size(64.dp)
            .background(Color(0xFFE6F4EA), CircleShape)
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = B360Green,
                modifier = Modifier.size(32.dp)
            )
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AndroidCustomLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) B360Green else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(Color(0xFFF0FDF4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = B360Green,
                modifier = Modifier.size(20.dp)
            )
        }

        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFFE2E8F0))
        )

        // Text Input
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                enabled = enabled,
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                decorationBox = { innerTextField: @Composable () -> Unit ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFF94A3B8), fontSize = 15.sp)
                    }
                    innerTextField()
                }
            )
        }

        if (isPassword && onPasswordToggle != null) {
            IconButton(
                onClick = onPasswordToggle,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onAuthenticated: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AuthViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.step) {
        if (state.step is AuthStep.Otp) {
            onLoginSuccess((state.step as AuthStep.Otp).userId)
        }
    }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onAuthenticated()
        }
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoHeader()

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .padding(top = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome Back!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F172A))
                        Text("Please sign in to continue", fontSize = 13.sp, color = Color(0xFF64748B))

                        if (state.error != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                }
                            }
                        }

                        AndroidCustomLoginTextField(
                            value = email,
                            onValueChange = { email = it; viewModel.dismissError() },
                            placeholder = "Email / Phone",
                            leadingIcon = Icons.Filled.Person,
                            enabled = !state.isLoading
                        )

                        AndroidCustomLoginTextField(
                            value = password,
                            onValueChange = { password = it; viewModel.dismissError() },
                            placeholder = "Password",
                            leadingIcon = Icons.Filled.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordToggle = { passwordVisible = !passwordVisible },
                            enabled = !state.isLoading
                        )

                        Button(
                            onClick = { viewModel.login(email, password) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text("Login / Ingia", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 16.sp)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).size(18.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text("OR", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val biometricManager = BiometricManager.from(context)
                                    val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                                    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                        viewModel.login("admin@biashara360.co.ke", "admin123")
                                    } else {
                                        val errorMsg = when (canAuthenticate) {
                                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware found on this device."
                                            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable."
                                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled. Please go to settings to configure fingerprint."
                                            else -> "Biometric authentication is not available."
                                        }
                                        viewModel.setError(errorMsg)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFE6F4EA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = B360Green, modifier = Modifier.size(22.dp))
                            }
                            Text("Login with biometrics", modifier = Modifier.weight(1f), color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Don't have an account? ", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text(
                                "Register",
                                color = B360Green,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onRegister() }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    FloatingShieldBadge()
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.step) {
        if (state.step is AuthStep.Otp) onRegistered()
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BusinessType.RETAIL) }

    val businessTypes = listOf(
        BusinessType.RETAIL to "Retail Seller",
        BusinessType.SERVICE to "Service Provider",
        BusinessType.HYBRID to "Hybrid Business",
        BusinessType.ONLINE_SELLER to "Online Seller"
    )

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { 
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White) 
                }
                Spacer(Modifier.width(8.dp))
                Text("Back", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            LogoHeader()

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .padding(top = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F172A), modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Jiunge na Biashara360", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.align(Alignment.CenterHorizontally))

                        if (state.error != null) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                    modifier = Modifier.padding(10.dp))
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; viewModel.dismissError() },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; viewModel.dismissError() },
                            label = { Text("Phone (07XX) *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; viewModel.dismissError() },
                            label = { Text("Email *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it; viewModel.dismissError() },
                            label = { Text("Business Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; viewModel.dismissError() },
                            label = { Text("Password *") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        Text("Business Type", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            businessTypes.forEach { (type, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedType = type }) {
                                    RadioButton(
                                        selected = selectedType == type,
                                        onClick = { selectedType = type },
                                        colors = RadioButtonDefaults.colors(selectedColor = B360Green),
                                        enabled = !state.isLoading
                                    )
                                    Text(label, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.register(name, phone, email, password, businessName, selectedType) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !state.isLoading && name.isNotBlank() && phone.isNotBlank() &&
                                email.isNotBlank() && businessName.isNotBlank() && password.isNotBlank()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text("Register / Jisajili", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 16.sp)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).size(18.dp))
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    FloatingShieldBadge()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    userId: String,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onVerified()
    }

    var otp by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("SMS") }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoHeader()

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .padding(top = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Two-Factor Authentication", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFF0F172A))
                        Text("Enter the OTP sent to your phone/email", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SMS", "Email", "App").forEach { ch ->
                                val isSelected = selectedChannel == ch
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedChannel = ch },
                                    label = { Text(ch, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = B360Green,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color(0xFF64748B)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = !state.isLoading,
                                        selected = isSelected,
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent
                                    ),
                                    enabled = !state.isLoading
                                )
                            }
                        }

                        if (state.error != null) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                    modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                            }
                        }

                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                            label = { Text("Enter 6-digit OTP") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = B360Green,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        Button(
                            onClick = { viewModel.verifyOtp(otp, selectedChannel) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(14.dp),
                            enabled = otp.length == 6 && !state.isLoading
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text("Verify / Thibitisha", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 16.sp)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).size(18.dp))
                                }
                            }
                        }

                        val cooldown = state.otpCooldownSeconds
                        TextButton(
                            onClick = { if (cooldown == 0) viewModel.resendOtp() },
                            enabled = cooldown == 0 && !state.isLoading
                        ) {
                            Text(
                                if (cooldown > 0) "Resend OTP in ${cooldown}s" else "Resend OTP",
                                color = if (cooldown > 0) Color.Gray else B360Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    FloatingShieldBadge()
                }
            }
        }
    }
}

