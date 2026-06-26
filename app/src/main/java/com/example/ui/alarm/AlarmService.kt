package com.example.ui.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarm: Alarm? = null
    private var autoStopJob: Job? = null

    private val NOTIFICATION_ID = 9999
    private val CHANNEL_ID = "OWNUP_ALARM_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        Log.d("AlarmService", "AlarmService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        Log.d("AlarmService", "onStartCommand with Alarm ID: $alarmId")

        if (alarmId != -1) {
            startForegroundServiceFlow(alarmId)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceFlow(alarmId: Int) {
        // First, build a quick placeholder notification so startForeground doesn't fail on newer Android versions
        createNotificationChannel()
        val notification = buildNotification("Morning Wake Up Alarm", "Time to own your morning!", null)
        startForeground(NOTIFICATION_ID, notification)

        // Then, fetch the real alarm in a coroutine and customize
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val alarm = withContext(Dispatchers.IO) {
                db.alarmDao().getAlarmById(alarmId)
            }

            if (alarm != null) {
                currentAlarm = alarm
                Log.d("AlarmService", "Found alarm: ${alarm.taskType}")

                // Update global alarm state holder
                AlarmStateHolder.startRinging(alarm)

                // Update notification with real details
                val updatedNotification = buildNotification(
                    title = "ALARM RINGING: ${alarm.taskType}",
                    text = alarm.taskDetails,
                    alarm = alarm
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)

                // Play Audio and Vibrate
                startAudio(alarm)
                startVibration(alarm)

                // Start automatic ring duration timeout
                scheduleAutoStop(alarm)
            } else {
                Log.e("AlarmService", "Alarm ID $alarmId not found in database")
                stopSelf()
            }
        }
    }

    private fun scheduleAutoStop(alarm: Alarm) {
        autoStopJob?.cancel()
        autoStopJob = serviceScope.launch {
            // Wait for ring duration (default 5 minutes)
            val durationMs = alarm.ringDurationMinutes * 60 * 1000L
            delay(durationMs)
            Log.d("AlarmService", "Auto-stop duration reached. Moving to Focus Mode!")
            
            // Auto transition to Focus Mode even if user missed it
            AlarmStateHolder.startFocusMode(
                alarmId = alarm.id,
                durationMinutes = alarm.activityDurationMinutes,
                taskType = alarm.taskType,
                taskDetails = alarm.taskDetails
            )
            AlarmStateHolder.stopRinging()
            stopSelf()
        }
    }

    private fun startAudio(alarm: Alarm) {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer?.stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri)
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
            Log.e("AlarmService", "Failed to play audio", e)
        }
    }

    private fun startVibration(alarm: Alarm) {
        if (!alarm.isVibrate) return

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800)
            val amplitudes = intArrayOf(0, 255, 0, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start vibration", e)
        }
    }

    private fun buildNotification(title: String, text: String, alarm: Alarm?): Notification {
        val fullScreenIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("LAUNCH_RINGING_ALARM", true)
            if (alarm != null) {
                putExtra("ALARM_ID", alarm.id)
            }
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OwnUp Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for ringing wake-up alarms in OwnUp"
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true) // Crucial to ring in Silent/DND mode!
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmService", "AlarmService destroyed")
        autoStopJob?.cancel()
        serviceJob.cancel()

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error cleaning up MediaPlayer", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error cleaning up Vibrator", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
