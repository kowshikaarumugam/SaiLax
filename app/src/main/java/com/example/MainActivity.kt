package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.alarm.AlarmViewModel
import com.example.ui.screens.AlarmRingingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PermissionAgreementScreen
import com.example.ui.screens.FocusModeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isAuthenticated by viewModel.isAuthenticated.collectAsState()
                val activeRingingAlarm by viewModel.activeRingingAlarm.collectAsState()
                val focusModeState by viewModel.focusModeState.collectAsState()

                // Dynamic permission checking
                var hasGrantedAllPermissions by remember { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // Re-check permissions every time the app resumes (e.g. returning from settings)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasGrantedAllPermissions = areAllPermissionsGranted()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        activeRingingAlarm != null -> {
                            AlarmRingingScreen(
                                alarm = activeRingingAlarm!!,
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        focusModeState != null -> {
                            FocusModeScreen(
                                focusState = focusModeState!!,
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        !isAuthenticated -> {
                            AuthScreen(
                                onAuthenticated = { email, name ->
                                    viewModel.authenticate(email, name)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        !hasGrantedAllPermissions -> {
                            PermissionAgreementScreen(
                                onAllGranted = {
                                    hasGrantedAllPermissions = true
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onAddAlarmClick = { /* Handled in dialog internally */ },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }

        return cameraGranted && locationGranted && notificationGranted && exactAlarmGranted && batteryOptimizationIgnored
    }
}
