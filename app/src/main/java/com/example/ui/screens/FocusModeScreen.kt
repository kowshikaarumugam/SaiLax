package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.data.model.Alarm
import com.example.ui.alarm.AlarmViewModel
import com.example.ui.alarm.FocusState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@SuppressLint("MissingPermission")
@Composable
fun FocusModeScreen(
    focusState: FocusState,
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Intercept back presses completely
    BackHandler(enabled = true) {
        // Do nothing - user is locked!
    }

    val isVerifying by viewModel.isVerifying.collectAsState()
    val verificationError by viewModel.verificationError.collectAsState()
    val verificationSuccess by viewModel.verificationSuccessMsg.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var base64Photo by remember { mutableStateOf<String?>(null) }
    var gpsLatitude by remember { mutableStateOf<Double?>(null) }
    var gpsLongitude by remember { mutableStateOf<Double?>(null) }
    var gpsStatus by remember { mutableStateOf("Ready to capture") }

    // Time calculations
    var remainingMillis by remember { mutableStateOf(focusState.endTimeMillis - System.currentTimeMillis()) }
    val isTimerComplete = remainingMillis <= 0

    LaunchedEffect(focusState.endTimeMillis) {
        while (remainingMillis > 0) {
            delay(1000)
            remainingMillis = focusState.endTimeMillis - System.currentTimeMillis()
        }
        remainingMillis = 0
    }

    // GPS location setup
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            gpsStatus = "Fetching GPS..."
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    gpsLatitude = location.latitude
                    gpsLongitude = location.longitude
                    gpsStatus = "GPS locked"
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            base64Photo = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Auto-trigger GPS lock
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gpsStatus = "Locking GPS..."
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        gpsLatitude = location.latitude
                        gpsLongitude = location.longitude
                        gpsStatus = "GPS Locked"
                    }
                }
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // Success Sound effect
    LaunchedEffect(verificationSuccess) {
        if (verificationSuccess != null) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val successPlayer = MediaPlayer.create(context, notificationUri)
                successPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Subtle pulsing animation for background
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0B1E), Color(0xFF060409))
                )
            )
    ) {
        // Subtle background light aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        contentDescription = "Focusing Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DISCIPLINE FOCUS ACTIVE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Own Your Habit Activity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Circular progress or Action camera
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .weight(1f)
            ) {
                if (!isTimerComplete) {
                    // Circular Countdown Timer
                    val minutes = remainingMillis / 60000
                    val seconds = (remainingMillis % 60000) / 1000
                    val text = String.format("%02d:%02d", minutes, seconds)

                    val progress = (remainingMillis.toFloat() / focusState.totalDurationMillis).coerceIn(0f, 1f)

                    CircularProgressIndicator(
                        progress = progress,
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 10.dp,
                        trackColor = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = text,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remaining",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Timer is complete! Display capture options.
                    if (capturedBitmap == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(1.1f)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(24.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed Activity",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "ACTIVITY TIME COMPLETE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Take a photo to verify and unlock",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Display captured preview
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Proof",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    capturedBitmap = null
                                    base64Photo = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Clear Photo", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Task information card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161520)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACTIVE TASK DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = focusState.taskType,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = focusState.taskDetails,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Actions or footer instructions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isTimerComplete) {
                    Text(
                        text = "Your device UI is locked until the countdown finishes. Build morning consistency and focus.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    if (capturedBitmap == null) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CAPTURE LIVE PROOF PHOTO", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (base64Photo != null) {
                                    val alarm = Alarm(
                                        focusState.alarmId,
                                        0,
                                        0,
                                        focusState.taskType,
                                        focusState.taskDetails,
                                        true,
                                        "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
                                        null,
                                        true,
                                        "",
                                        5,
                                        30,
                                        focusState.referencePhotoPath
                                    )
                                    viewModel.verifyProofPhoto(
                                        alarm = alarm,
                                        base64Photo = base64Photo!!,
                                        gpsLat = gpsLatitude,
                                        gpsLng = gpsLongitude,
                                        photoPath = "focus_proof_${System.currentTimeMillis()}.jpg"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Psychology, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBMIT FOR AI COMPARISON", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { viewModel.clearFocusMode() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.15f))
                ) {
                    Text("Temporary Bypass (Testing)", fontSize = 11.sp)
                }
            }
        }

        // Overlay states (copied aesthetics from AlarmRingingScreen)
        if (isVerifying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI is comparing with reference baseline...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Applying object and scene similarity metrics",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (verificationSuccess != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "PROOF VERIFIED & MATCHED!",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        verificationSuccess!!,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (verificationError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Failed Verification",
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "VERIFICATION REJECTED",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        verificationError!!,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            capturedBitmap = null
                            base64Photo = null
                            // Resets error
                            viewModel.startFocusMode(
                                focusState.alarmId,
                                0, // 0 minutes means camera immediately accessible
                                focusState.taskType,
                                focusState.taskDetails
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TRY AGAIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
