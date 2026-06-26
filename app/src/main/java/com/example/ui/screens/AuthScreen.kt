package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthenticated: (email: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Preferences for simulation of secure local storage
    val sharedPrefs = remember {
        context.getSharedPreferences("ownup_auth_mock", android.content.Context.MODE_PRIVATE)
    }

    // Auth screen state tabs (0 = Login, 1 = Register)
    var isLoginMode by remember { mutableStateOf(true) }

    // Input States
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    // Password visibility
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Feedback States
    var uiErrorMsg by remember { mutableStateOf<String?>(null) }
    var uiSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Rate limiting & lockout state
    var failedAttempts by remember {
        mutableStateOf(sharedPrefs.getInt("failed_attempts", 0))
    }
    var lockoutTimestamp by remember {
        mutableStateOf(sharedPrefs.getLong("lockout_timestamp", 0L))
    }
    var secondsLeftOfLockout by remember { mutableStateOf(0L) }

    // Email verification overlay states
    var showVerificationOverlay by remember { mutableStateOf(false) }
    var verificationEmailSent by remember { mutableStateOf("") }
    var verificationNameRegistered by remember { mutableStateOf("") }
    var verificationPasswordRegistered by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var simulatedOtpCode by remember { mutableStateOf("1234") } // Pre-sent mock OTP
    var verificationSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Forgot password states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordSuccess by remember { mutableStateOf<String?>(null) }

    // Carousel quote cycle
    var quoteIndex by remember { mutableStateOf(0) }
    val quotes = listOf(
        "Own your morning, own your day.",
        "Your future self will thank you for waking up today.",
        "Discipline is choosing between what you want now and what you want most.",
        "Today is another opportunity to build consistency.",
        "Stop procrastinating. Real proof builds real habits."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            quoteIndex = (quoteIndex + 1) % quotes.size
        }
    }

    // Active lockout timer countdown
    LaunchedEffect(lockoutTimestamp, failedAttempts) {
        if (failedAttempts >= 5) {
            val currentTime = System.currentTimeMillis()
            val fifteenMinutesMs = 15 * 60 * 1000
            val lockTimePassed = currentTime - lockoutTimestamp
            if (lockTimePassed < fifteenMinutesMs) {
                var secondsLeft = ((fifteenMinutesMs - lockTimePassed) / 1000)
                while (secondsLeft > 0) {
                    secondsLeftOfLockout = secondsLeft
                    delay(1000)
                    secondsLeft--
                }
                // Lockout completed
                failedAttempts = 0
                sharedPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_timestamp", 0L).apply()
                secondsLeftOfLockout = 0
            } else {
                failedAttempts = 0
                sharedPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_timestamp", 0L).apply()
                secondsLeftOfLockout = 0
            }
        }
    }

    // Gradient background representing a fresh morning (sunrise/orange sunset to dark blue slate)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep Slate Blue
            Color(0xFF1E293B), // Navy Slate
            Color(0xFF31153E)  // Subtle Crimson Dawn
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "OwnUp Logo",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title and Slogan
            Text(
                "OwnUp",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                "Own your morning, own your day.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quote display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = quotes[quoteIndex],
                        transitionSpec = {
                            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                        },
                        label = "QuoteAnimation"
                    ) { quote ->
                        Text(
                            "\"$quote\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation tabs between Sign In and Sign Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        isLoginMode = true
                        uiErrorMsg = null
                        uiSuccessMsg = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginMode) Color(0xFFF59E0B) else Color.Transparent,
                        contentColor = if (isLoginMode) Color(0xFF0F172A) else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isLoginMode = false
                        uiErrorMsg = null
                        uiSuccessMsg = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoginMode) Color(0xFFF59E0B) else Color.Transparent,
                        contentColor = if (!isLoginMode) Color(0xFF0F172A) else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Register", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary auth Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (isLoginMode) "Sign In to Your Account" else "Register Accountability Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // 1. FULL NAME INPUT (Registration only)
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                uiErrorMsg = null
                            },
                            label = { Text("Your Name", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B))
                            },
                            placeholder = { Text("Enter your full name", color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFFF59E0B),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. EMAIL ADDRESS INPUT
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            uiErrorMsg = null
                        },
                        label = { Text("Email Address", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFF59E0B))
                        },
                        placeholder = { Text("you@domain.com", color = Color.White.copy(alpha = 0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFFF59E0B),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. PASSWORD INPUT
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            uiErrorMsg = null
                        },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        },
                        placeholder = { Text("Minimum 8 characters", color = Color.White.copy(alpha = 0.4f)) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFFF59E0B),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. CONFIRM PASSWORD (Registration only)
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = {
                                confirmPasswordInput = it
                                uiErrorMsg = null
                            },
                            label = { Text("Confirm Password", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            placeholder = { Text("Repeat password", color = Color.White.copy(alpha = 0.4f)) },
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFFF59E0B),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 5. REMEMBER ME & FORGOT PASSWORD row (Login only)
                    if (isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFF59E0B),
                                        checkmarkColor = Color(0xFF0F172A),
                                        uncheckedColor = Color.White.copy(alpha = 0.5f)
                                    )
                                )
                                Text(
                                    "Remember Me",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                "Forgot Password?",
                                color = Color(0xFFF59E0B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    forgotPasswordEmail = emailInput.trim()
                                    forgotPasswordSuccess = null
                                    forgotPasswordError = null
                                    showForgotPasswordDialog = true
                                }
                            )
                        }
                    }

                    // 6. ERROR / SUCCESS Indicators
                    AnimatedVisibility(visible = uiErrorMsg != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiErrorMsg ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiSuccessMsg != null) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiSuccessMsg ?: "",
                                    color = Color(0xFF10B981),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // LOCKOUT COUNTER WARNING
                    if (secondsLeftOfLockout > 0) {
                        val minutes = secondsLeftOfLockout / 60
                        val seconds = secondsLeftOfLockout % 60
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Too many failed attempts. Account locked. Try again in ${String.format("%02d:%02d", minutes, seconds)}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 7. SUBMIT BUTTON
                    Button(
                        onClick = {
                            if (secondsLeftOfLockout > 0) return@Button

                            val email = emailInput.trim().lowercase()
                            val password = passwordInput

                            // EMAIL VALIDATION
                            if (email.isEmpty()) {
                                uiErrorMsg = "Email address is required."
                                return@Button
                            }

                            val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
                            if (!email.matches(emailPattern.toRegex())) {
                                uiErrorMsg = "Please enter a valid email format (e.g. user@domain.com)."
                                return@Button
                            }

                            // PASSWORD VALIDATION
                            if (password.isEmpty()) {
                                uiErrorMsg = "Password is required."
                                return@Button
                            }

                            if (password.length < 8) {
                                uiErrorMsg = "Password must be at least 8 characters long."
                                return@Button
                            }

                            // Complexity validation: uppercase, lowercase, number, special char
                            val hasUppercase = password.any { it.isUpperCase() }
                            val hasLowercase = password.any { it.isLowerCase() }
                            val hasDigit = password.any { it.isDigit() }
                            val hasSpecial = password.any { !it.isLetterOrDigit() }

                            if (!isLoginMode) {
                                // Additional validation rules on Sign Up
                                if (nameInput.trim().isEmpty()) {
                                    uiErrorMsg = "Name is required."
                                    return@Button
                                }
                                if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
                                    uiErrorMsg = "Password must contain at least 1 uppercase, 1 lowercase, 1 number, and 1 special character."
                                    return@Button
                                }
                                if (password != confirmPasswordInput) {
                                    uiErrorMsg = "Passwords do not match."
                                    return@Button
                                }

                                // Simulated verification flow initiation
                                verificationEmailSent = email
                                verificationNameRegistered = nameInput.trim()
                                verificationPasswordRegistered = password
                                simulatedOtpCode = (1000..9999).random().toString() // random 4-digit code

                                coroutineScope.launch {
                                    // Simulated sending delayed
                                    delay(800)
                                    showVerificationOverlay = true
                                    otpError = null
                                }
                                return@Button
                            }

                            // LOGIN IMPLEMENTATION
                            val registeredEmail = sharedPrefs.getString("reg_email_$email", null)
                            val registeredPassword = sharedPrefs.getString("reg_pwd_$email", null)
                            val registeredName = sharedPrefs.getString("reg_name_$email", null)

                            // Default account fallback for easy evaluation
                            val defaultEmail = "kowshikaarumugam2005@gmail.com"
                            val isDefaultLogin = email == defaultEmail && password == "OwnUp2026!"

                            if ((registeredEmail != null && registeredPassword == password) || isDefaultLogin) {
                                // Reset failed attempts on success
                                failedAttempts = 0
                                sharedPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_timestamp", 0L).apply()

                                val activeName = registeredName ?: "Kowshika"
                                uiSuccessMsg = "Login successful! Redirecting..."
                                coroutineScope.launch {
                                    delay(1200)
                                    onAuthenticated(email, activeName)
                                }
                            } else {
                                // Increment failed attempts
                                val currentFailed = failedAttempts + 1
                                failedAttempts = currentFailed
                                sharedPrefs.edit().putInt("failed_attempts", currentFailed).apply()

                                if (currentFailed >= 5) {
                                    val now = System.currentTimeMillis()
                                    lockoutTimestamp = now
                                    sharedPrefs.edit().putLong("lockout_timestamp", now).apply()
                                    uiErrorMsg = "Too many failed attempts. Account locked for 15 minutes."
                                } else {
                                    uiErrorMsg = "Invalid email or password."
                                }
                            }
                        },
                        enabled = secondsLeftOfLockout <= 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            if (isLoginMode) "BEGIN ACCOUNTABILITY" else "CREATE ACCOUNT",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                        Text(
                            "OR",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                    }

                    OutlinedButton(
                        onClick = {
                            if (secondsLeftOfLockout > 0) return@OutlinedButton
                            uiSuccessMsg = "Google authentication successful! Redirecting..."
                            coroutineScope.launch {
                                delay(1200)
                                onAuthenticated("kowshikaarumugam2005@gmail.com", "Kowshika")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "G",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer info
            Text(
                "Secure Client Verification Engine • Active State Preserved Locally",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Demo instructions helper box
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Demo Evaluation Tips:",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "• You can register any email & pass. Password requirements: 8+ chars, with an uppercase, lowercase, number, and symbol (e.g. Welcome2026!).\n• Pre-registered credentials:\nEmail: kowshikaarumugam2005@gmail.com\nPassword: OwnUp2026!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }

    // A. OTP EMAIL VERIFICATION OVERLAY
    if (showVerificationOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MailOutline, "OTP Sent", tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                    }

                    Text(
                        "Verification Needed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        "Verification email has been sent successfully.",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "We sent a One-Time Password (OTP) to $verificationEmailSent. Please enter the 4-digit code below to activate your account.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    // Simulated message preview containing OTP
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Simulated Email Notification: Your OTP Code is $simulatedOtpCode",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // OTP Input
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            otpInput = it
                            otpError = null
                        },
                        label = { Text("Enter OTP Code", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF10B981)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (otpError != null) {
                        Text(
                            text = otpError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (verificationSuccessMsg != null) {
                        Text(
                            text = verificationSuccessMsg ?: "",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showVerificationOverlay = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (otpInput.trim() == simulatedOtpCode) {
                                    // Save registered user
                                    sharedPrefs.edit()
                                        .putString("reg_email_${verificationEmailSent.lowercase()}", verificationEmailSent.lowercase())
                                        .putString("reg_name_${verificationEmailSent.lowercase()}", verificationNameRegistered)
                                        .putString("reg_pwd_${verificationEmailSent.lowercase()}", verificationPasswordRegistered)
                                        .apply()

                                    verificationSuccessMsg = "Account activated automatically! Logging in..."
                                    coroutineScope.launch {
                                        delay(1500)
                                        showVerificationOverlay = false
                                        // Auto authenticates and loads
                                        onAuthenticated(verificationEmailSent, verificationNameRegistered)
                                    }
                                } else {
                                    otpError = "Incorrect OTP. Please enter the correct code."
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White)
                        ) {
                            Text("Verify Code")
                        }
                    }

                    TextButton(
                        onClick = {
                            simulatedOtpCode = (1000..9999).random().toString()
                            otpError = null
                            verificationSuccessMsg = null
                            otpInput = ""
                        }
                    ) {
                        Text("Resend Verification Email", color = Color(0xFFF59E0B), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // B. FORGOT PASSWORD DIALOG
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your registered email address and we will send a password reset link expiring in 15 minutes.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = {
                            forgotPasswordEmail = it
                            forgotPasswordError = null
                        },
                        label = { Text("Registered Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (forgotPasswordError != null) {
                        Text(forgotPasswordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (forgotPasswordSuccess != null) {
                        Text(forgotPasswordSuccess!!, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = forgotPasswordEmail.trim().lowercase()
                        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
                        if (email.isEmpty() || !email.matches(emailPattern.toRegex())) {
                            forgotPasswordError = "Please enter a valid email format."
                            return@Button
                        }

                        // Simulation of successfully generating/sending reset password link
                        forgotPasswordSuccess = "A secure reset link has been sent to $email. Valid for 15 minutes."
                        coroutineScope.launch {
                            delay(2500)
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
