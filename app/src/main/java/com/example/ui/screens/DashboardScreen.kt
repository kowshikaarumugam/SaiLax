package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Alarm
import com.example.data.model.HabitStreak
import com.example.data.model.ProofLog
import com.example.ui.alarm.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AlarmViewModel,
    onAddAlarmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.allAlarms.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val streaks by viewModel.allStreaks.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Alarms, 1: Streaks, 2: History
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "OwnUp",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (userName.isNotEmpty()) "Rise and shine, $userName! Own your day." else "Own your morning, own your day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, "Add Alarm") },
                    text = { Text("Set Alarm") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Stat Banner
            StreakSummaryBanner(streaks = streaks)

            // Tabs Header
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Alarms (${alarms.size})") },
                    icon = { Icon(Icons.Default.Alarm, null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("My Streaks") },
                    icon = { Icon(Icons.Default.LocalFireDepartment, null) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("AI History") },
                    icon = { Icon(Icons.Default.History, null) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area based on tab
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> AlarmsTabContent(
                        alarms = alarms,
                        onToggle = { viewModel.toggleAlarm(it) },
                        onDelete = { viewModel.deleteAlarm(it) },
                        onTestFire = { viewModel.triggerRinging(it) }
                    )
                    1 -> StreaksTabContent(streaks = streaks)
                    2 -> HistoryTabContent(logs = logs)
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onSave = { alarm ->
                viewModel.insertAlarm(alarm)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StreakSummaryBanner(streaks: List<HabitStreak>) {
    val totalStreak = streaks.sumOf { it.currentStreak }
    val studyStreak = streaks.find { it.taskType == "Study" }?.currentStreak ?: 0
    val gymStreak = streaks.find { it.taskType == "Gym" }?.currentStreak ?: 0
    val readingStreak = streaks.find { it.taskType == "Reading" }?.currentStreak ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Total Habit Points",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$totalStreak Days",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Keep up the morning accountability!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreakIndicator(label = "Study", value = studyStreak, color = Color(0xFF3B82F6))
                StreakIndicator(label = "Gym", value = gymStreak, color = Color(0xFF10B981))
                StreakIndicator(label = "Read", value = readingStreak, color = Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
fun StreakIndicator(label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "$value",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AlarmsTabContent(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onTestFire: (Alarm) -> Unit
) {
    if (alarms.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AlarmOff,
                contentDescription = "No Alarms",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No accountability alarms yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Set an alarm for Study, Gym, or Reading and force yourself to follow through with real-time AI verification!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { onToggle(alarm) },
                    onDelete = { onDelete(alarm) },
                    onTestFire = { onTestFire(alarm) }
                )
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTestFire: () -> Unit
) {
    val formattedTime = remember(alarm.hour, alarm.minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    val categoryColor = when (alarm.taskType) {
        "Study" -> Color(0xFF3B82F6)
        "Gym" -> Color(0xFF10B981)
        "Reading" -> Color(0xFFF59E0B)
        else -> Color(0xFF8B5CF6)
    }

    val categoryIcon = when (alarm.taskType) {
        "Study" -> Icons.Default.School
        "Gym" -> Icons.Default.FitnessCenter
        "Reading" -> Icons.Default.Book
        else -> Icons.Default.Task
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = alarm.taskType,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        alarm.taskType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Proof requirement:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        alarm.taskDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onTestFire,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Alarm",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun StreaksTabContent(streaks: List<HabitStreak>) {
    val categories = listOf("Study", "Gym", "Reading", "Custom")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            val streakData = streaks.find { it.taskType == category } ?: HabitStreak(category)
            val color = when (category) {
                "Study" -> Color(0xFF3B82F6)
                "Gym" -> Color(0xFF10B981)
                "Reading" -> Color(0xFFF59E0B)
                else -> Color(0xFF8B5CF6)
            }
            val icon = when (category) {
                "Study" -> Icons.Default.School
                "Gym" -> Icons.Default.FitnessCenter
                "Reading" -> Icons.Default.Book
                else -> Icons.Default.Task
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                        }

                        Column {
                            Text(category, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                streakData.lastSuccessDate?.let { "Last proof: $it" } ?: "No logs captured yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, "Streak", tint = color, modifier = Modifier.size(24.dp))
                            Text(
                                "${streakData.currentStreak}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Text(
                            "Best: ${streakData.longestStreak}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(logs: List<ProofLog>) {
    if (logs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.HistoryToggleOff,
                null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No validation history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "When you wake up and submit proof, the AI evaluations, confidence metrics, and GPS coords will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                val formattedDate = remember(log.timestamp) {
                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                }

                val statusColor = if (log.status == "PASSED") Color(0xFF10B981) else Color(0xFFEF4444)

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                log.taskType,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    log.status,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }

                        Text(
                            formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            "Requirement: ${log.taskDetails}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Psychology, "AI", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "AI Review:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            log.reviewNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (log.gpsLatitude != null && log.gpsLongitude != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, "GPS", modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                Text(
                                    "GPS Verified: ${String.format("%.4f", log.gpsLatitude)}, ${String.format("%.4f", log.gpsLongitude)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var isAm by remember { mutableStateOf(true) }
    var taskType by remember { mutableStateOf("Study") }
    var taskDetails by remember { mutableStateOf("Take a photo of open textbook or notebooks on your study desk.") }
    
    // New fields
    var ringDurationMinutes by remember { mutableStateOf(5) }
    var activityDurationMinutes by remember { mutableStateOf(30) }
    var referencePhotoPath by remember { mutableStateOf<String?>(null) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val categories = listOf("Study", "Gym", "Reading", "Custom")
    val ringOptions = listOf(2, 5, 10, 15)
    val activityOptions = listOf(15, 30, 45, 60, 120)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            referenceBitmap = bitmap
            try {
                val file = File(context.filesDir, "baseline_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                referencePhotoPath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B24))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Create Accountability Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Time Pick Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { hour = if (hour == 12) 1 else hour + 1 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Increment hour", tint = Color.White)
                            }
                            Text(
                                text = String.format("%02d", hour),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = { hour = if (hour == 1) 12 else hour - 1 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Decrement hour", tint = Color.White)
                            }
                        }

                        Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))

                        // Minute Spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Increment minute", tint = Color.White)
                            }
                            Text(
                                text = String.format("%02d", minute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = { minute = if (minute == 0) 55 else (minute - 5 + 60) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Decrement minute", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM/PM Switch
                        Column {
                            Button(
                                onClick = { isAm = !isAm },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAm) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(if (isAm) "AM" else "PM")
                            }
                        }
                    }
                }

                // Task Selector chips
                item {
                    Text("Select Habit Category:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = taskType == category
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    taskType = category
                                    taskDetails = when (category) {
                                        "Study" -> "Take a photo of open textbook or notebooks on your study desk."
                                        "Gym" -> "Take a photo of weights, workout bench, treadmill, or dumbbells in gym."
                                        "Reading" -> "Take a photo of a printed book page or a reader device."
                                        else -> "Custom proof: Please take a photo as described."
                                    }
                                },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    labelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                // Proof details text field
                item {
                    OutlinedTextField(
                        value = taskDetails,
                        onValueChange = { taskDetails = it },
                        label = { Text("What proof is required to shut it off?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                // Alarm Ring Duration Configuration
                item {
                    Text("Alarm Ring Duration (Minutes):", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ringOptions.forEach { mins ->
                            val isSelected = ringDurationMinutes == mins
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { ringDurationMinutes = mins },
                                label = { Text("$mins Mins") },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Activity Verification Duration Configuration
                item {
                    Text("Focus Activity Duration (Minutes):", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activityOptions.forEach { mins ->
                            val isSelected = activityDurationMinutes == mins
                            val labelText = if (mins >= 60) "${mins / 60} Hr" else "$mins Mins"
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { activityDurationMinutes = mins },
                                label = { Text(labelText) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Reference Baseline Photo capturing
                item {
                    Text("Reference Baseline Photo (Optional):", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (referenceBitmap == null) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CAPTURE REFERENCE BASELINE", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = referenceBitmap!!.asImageBitmap(),
                                    contentDescription = "Baseline image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Baseline Set", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("Will be matched using AI similarity", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = {
                                referenceBitmap = null
                                referencePhotoPath = null
                            }) {
                                Icon(Icons.Default.Delete, "Delete baseline", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Dialog Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val finalHour = when {
                                    isAm && hour == 12 -> 0
                                    !isAm && hour != 12 -> hour + 12
                                    else -> hour
                                }
                                onSave(
                                    Alarm(
                                        hour = finalHour,
                                        minute = minute,
                                        taskType = taskType,
                                        taskDetails = taskDetails,
                                        ringDurationMinutes = ringDurationMinutes,
                                        activityDurationMinutes = activityDurationMinutes,
                                        referencePhotoPath = referencePhotoPath
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Active", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
