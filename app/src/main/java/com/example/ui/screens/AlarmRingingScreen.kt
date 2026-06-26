package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.ui.alarm.AlarmService
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@SuppressLint("MissingPermission")
@Composable
fun AlarmRingingScreen(
    alarm: Alarm,
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isVerifying by viewModel.isVerifying.collectAsState()
    val verificationError by viewModel.verificationError.collectAsState()
    val verificationSuccess by viewModel.verificationSuccessMsg.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var base64Photo by remember { mutableStateOf<String?>(null) }
    var gpsLatitude by remember { mutableStateOf<Double?>(null) }
    var gpsLongitude by remember { mutableStateOf<Double?>(null) }
    var gpsStatus by remember { mutableStateOf("Ready to capture") }

    // Countdown: 60 minutes in seconds = 3600
    var secondsRemaining by remember { mutableStateOf(3600) }

    // Pulsing animation for the warning layout
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Alarm ringtone & Vibrator management
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var vibrator by remember { mutableStateOf<Vibrator?>(null) }

    // GPS location fetcher
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            gpsStatus = "Fetching GPS location..."
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    gpsLatitude = location.latitude
                    gpsLongitude = location.longitude
                    gpsStatus = "GPS locked: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                } else {
                    gpsStatus = "GPS lock failed (No last location). Using simulated GPS."
                    gpsLatitude = 37.7749
                    gpsLongitude = -122.4194
                }
            }.addOnFailureListener {
                gpsStatus = "GPS lock error: ${it.message}. Using simulated GPS."
                gpsLatitude = 37.7749
                gpsLongitude = -122.4194
            }
        } else {
            gpsStatus = "Location denied. Using simulated GPS."
            gpsLatitude = 37.7749
            gpsLongitude = -122.4194
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            // Convert to Base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            base64Photo = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Trigger Location update
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gpsStatus = "Fetching GPS..."
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        gpsLatitude = location.latitude
                        gpsLongitude = location.longitude
                        gpsStatus = "GPS locked: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                    } else {
                        gpsLatitude = 37.7749
                        gpsLongitude = -122.4194
                        gpsStatus = "Simulated GPS"
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

    // Initialize/Cleanup Audio + Vibration
    DisposableEffect(Unit) {
        // Play Alarm Ringtone
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start Vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 800, 400, 800)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 800, 400, 800), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            vibrator?.cancel()
        }
    }

    // Countdown effect
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    // Success Screen Auto-Dismiss
    LaunchedEffect(verificationSuccess) {
        if (verificationSuccess != null) {
            // Success sound / chime!
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                vibrator?.cancel()

                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val successPlayer = MediaPlayer.create(context, notificationUri)
                successPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(3500)
            viewModel.dismissRinging()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0C15)) // Ultra dark background for alarm layout
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header (Warning & Timer)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.1f), CircleShape)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Ringing Alarm Warning",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "DISCIPLINE LOCK ENABLED",
                    color = Color(0xFFFF3B30),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                Text(
                    "Phone Auto-Lockout: ${String.format("%02d:%02d", minutes, seconds)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = secondsRemaining / 3600f,
                    color = Color(0xFFFF3B30),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                )
            }

            // 2. Middle Body (Details & Proof Camera Preview)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C2A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "REQUIRED HABIT ACTIVITY",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            alarm.taskType,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            alarm.taskDetails,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Photo Preview
                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        gpsStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        // High-Priority Focus Mode Action Button
                        Button(
                            onClick = {
                                // Stop the ringing sound/service
                                val stopServiceIntent = Intent(context, AlarmService::class.java)
                                context.stopService(stopServiceIntent)

                                // Trigger focus mode flow in viewmodel
                                viewModel.startFocusMode(
                                    alarmId = alarm.id,
                                    durationMinutes = alarm.activityDurationMinutes,
                                    taskType = alarm.taskType,
                                    taskDetails = alarm.taskDetails,
                                    referencePhotoPath = alarm.referencePhotoPath
                                )
                                // Shut off ringing state
                                viewModel.dismissRinging()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.HourglassEmpty, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I'M AWAKE / START FOCUS ACTIVITY", fontWeight = FontWeight.Bold)
                        }

                        // Secondary action if they want to capture proof immediately
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBMIT PROOF DIRECTLY", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. Footer (Submit Button / Info)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (capturedBitmap != null) {
                    Button(
                        onClick = {
                            if (base64Photo != null) {
                                viewModel.verifyProofPhoto(
                                    alarm = alarm,
                                    base64Photo = base64Photo!!,
                                    gpsLat = gpsLatitude,
                                    gpsLng = gpsLongitude,
                                    photoPath = "captured_proof_${System.currentTimeMillis()}.jpg"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Psychology, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFY WITH AI", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Text(
                        "Start your active focus timer or submit proof directly to silence this alarm. No bypass allowed.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Graceful Bypass button
                TextButton(
                    onClick = {
                        val stopServiceIntent = Intent(context, AlarmService::class.java)
                        context.stopService(stopServiceIntent)
                        viewModel.dismissRinging()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Emergency Dismiss (Testing)", fontSize = 11.sp)
                }
            }
        }

        // Verification Status Overlays
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
                        "Gemini is analyzing your proof...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Checking photo EXIF matching context",
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
                        "PROOF ACCEPTED!",
                        color = Color.White,
                        fontSize = 24.sp,
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Habit streak incremented! 🔥",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                        "PROOF REJECTED",
                        color = Color.White,
                        fontSize = 24.sp,
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
                        onClick = { viewModel.triggerRinging(alarm) }, // Re-trigger normal screen
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
