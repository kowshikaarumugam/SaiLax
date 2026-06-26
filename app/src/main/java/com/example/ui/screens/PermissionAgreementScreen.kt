package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun PermissionAgreementScreen(
    onAllGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var cameraGranted by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }
    var exactAlarmGranted by remember { mutableStateOf(false) }
    var batteryOptimizationIgnored by remember { mutableStateOf(false) }

    fun checkPermissions() {
        cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    // Check on launch
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Standard runtime permissions launcher
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkPermissions()
    }

    // Direct user settings monitor
    DisposableEffect(Unit) {
        // Simple periodic check when user returns to app
        checkPermissions()
        onDispose {}
    }

    // If everything is already perfect, allow proceeding!
    val allPermissionsGranted = cameraGranted && locationGranted && notificationGranted && exactAlarmGranted && batteryOptimizationIgnored

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17)) // Deep professional slate/dark theme
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "OWNUP ACCOUNTABILITY",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Morning Lock Permissions",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To make sure your alarm cannot be bypassed and rings precisely when required, OwnUp requires the following parameters. Tap each item to grant.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // List of Permissions
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionItemCard(
                    title = "Camera Live Capture",
                    description = "Used to analyze visual proof of your starting habits (AI model validation).",
                    icon = Icons.Default.PhotoCamera,
                    isGranted = cameraGranted,
                    onClick = {
                        runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Exact Alarm Timing",
                    description = "Required to sound alarms exactly at the set time without system delays.",
                    icon = Icons.Default.AccessTime,
                    isGranted = exactAlarmGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Unrestricted Background Execution",
                    description = "Prevents Android battery managers from sleeping or suppressing morning alarms.",
                    icon = Icons.Default.BatterySaver,
                    isGranted = batteryOptimizationIgnored,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Notification Delivery",
                    description = "Sounds persistent warnings, alarm updates, and keeps focus status in context.",
                    icon = Icons.Default.NotificationsActive,
                    isGranted = notificationGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "GPS Proof Validation",
                    description = "Records location coordinates with your submitted proof for spatial anti-bypass.",
                    icon = Icons.Default.LocationOn,
                    isGranted = locationGranted,
                    onClick = {
                        runtimePermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (allPermissionsGranted) {
                    onAllGranted()
                } else {
                    checkPermissions()
                    // Prompt standard remaining ones
                    val permissionsNeeded = mutableListOf<String>()
                    if (!cameraGranted) permissionsNeeded.add(Manifest.permission.CAMERA)
                    if (!locationGranted) {
                        permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    if (permissionsNeeded.isNotEmpty()) {
                        runtimePermissionsLauncher.launch(permissionsNeeded.toTypedArray())
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allPermissionsGranted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                contentColor = if (allPermissionsGranted) Color.White else Color.White.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (allPermissionsGranted) {
                Icon(Icons.Default.LockOpen, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ENTER OWNUP DASHBOARD", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Security, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GRANT MISSING PERMISSIONS", fontWeight = FontWeight.Bold)
            }
        }

        if (!allPermissionsGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onAllGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PROCEED IN COMPATIBILITY MODE (LITE)",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF141F1F) else Color(0xFF1D1B24)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isGranted) "GRANTED" else "TAP TO GRANT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }
    }
}
