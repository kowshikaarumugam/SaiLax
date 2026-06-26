package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.Alarm
import com.example.data.model.HabitStreak
import com.example.data.model.ProofLog
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected

    private val _syncStatus = MutableStateFlow("Local-only mode (Awaiting connection)")
    val syncStatus: StateFlow<String> = _syncStatus

    init {
        checkFirebaseAvailability()
    }

    fun isFirebaseAvailable(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun checkFirebaseAvailability() {
        val available = isFirebaseAvailable()
        _isFirebaseConnected.value = available
        _syncStatus.value = if (available) {
            "Cloud Sync Ready (Firebase Active)"
        } else {
            "Local Mode (Firebase options missing. Running securely on local Room DB)"
        }
        Log.d(TAG, "Firebase Availability checked: $available")
    }

    suspend fun syncDataToFirestore(
        context: Context,
        alarms: List<Alarm>,
        streaks: List<HabitStreak>,
        logs: List<ProofLog>
    ) {
        if (!isFirebaseAvailable()) {
            _syncStatus.value = "Local DB Active (Room Persistence)"
            return
        }

        try {
            _syncStatus.value = "Syncing with Firestore..."
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user == null) {
                _syncStatus.value = "Cloud Sync Paused: Log in to sync"
                return
            }

            val db = FirebaseFirestore.getInstance()
            val userId = user.uid

            // Sync Alarms
            val alarmsCollection = db.collection("users").document(userId).collection("alarms")
            for (alarm in alarms) {
                val alarmMap = mapOf(
                    "id" to alarm.id,
                    "hour" to alarm.hour,
                    "minute" to alarm.minute,
                    "label" to alarm.label,
                    "isEnabled" to alarm.isEnabled,
                    "taskType" to alarm.taskType,
                    "taskDetails" to alarm.taskDetails,
                    "activityDurationMinutes" to alarm.activityDurationMinutes,
                    "referencePhotoPath" to alarm.referencePhotoPath,
                    "daysOfWeek" to alarm.daysOfWeek,
                    "isVibrate" to alarm.isVibrate,
                    "ringDurationMinutes" to alarm.ringDurationMinutes,
                    "ringtoneUri" to alarm.ringtoneUri
                )
                alarmsCollection.document(alarm.id.toString()).set(alarmMap).await()
            }

            // Sync Streaks
            val streaksCollection = db.collection("users").document(userId).collection("streaks")
            for (streak in streaks) {
                val streakMap = mapOf(
                    "taskType" to streak.taskType,
                    "currentStreak" to streak.currentStreak,
                    "longestStreak" to streak.longestStreak,
                    "lastSuccessDate" to streak.lastSuccessDate
                )
                streaksCollection.document(streak.taskType).set(streakMap).await()
            }

            // Sync Logs
            val logsCollection = db.collection("users").document(userId).collection("logs")
            for (log in logs) {
                val logMap = mapOf(
                    "id" to log.id,
                    "alarmId" to log.alarmId,
                    "taskType" to log.taskType,
                    "taskDetails" to log.taskDetails,
                    "timestamp" to log.timestamp,
                    "status" to log.status,
                    "reviewNotes" to log.reviewNotes,
                    "gpsLatitude" to log.gpsLatitude,
                    "gpsLongitude" to log.gpsLongitude,
                    "confidence" to log.confidence,
                    "photoPath" to log.photoPath
                )
                logsCollection.document(log.id.toString()).set(logMap).await()
            }

            _syncStatus.value = "Cloud Sync Success: All data secure!"
        } catch (e: Exception) {
            _syncStatus.value = "Cloud Sync Failed: ${e.localizedMessage}"
            Log.e(TAG, "Error syncing to Firestore", e)
        }
    }
}
