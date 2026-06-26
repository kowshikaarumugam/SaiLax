package com.example.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Alarm
import com.example.data.model.ProofLog
import com.example.data.model.HabitStreak
import com.example.data.repository.AlarmRepository
import com.example.data.repository.ProofRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val alarmRepo = AlarmRepository(db.alarmDao())
    private val proofRepo = ProofRepository(db.proofLogDao(), db.habitStreakDao())

    private val sharedPrefs = application.getSharedPreferences("ownup_prefs", android.content.Context.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        val savedEmail = sharedPrefs.getString("user_email", "") ?: ""
        val savedName = sharedPrefs.getString("user_name", "") ?: ""
        if (savedEmail.isNotEmpty()) {
            _userEmail.value = savedEmail
            _userName.value = savedName
            _isAuthenticated.value = true
        }
    }

    fun authenticate(email: String, name: String) {
        sharedPrefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()
        _userEmail.value = email
        _userName.value = name
        _isAuthenticated.value = true
    }

    fun logout() {
        sharedPrefs.edit()
            .remove("user_email")
            .remove("user_name")
            .apply()
        _userEmail.value = ""
        _userName.value = ""
        _isAuthenticated.value = false
    }

    val allAlarms: StateFlow<List<Alarm>> = alarmRepo.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<ProofLog>> = proofRepo.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStreaks: StateFlow<List<HabitStreak>> = proofRepo.allStreaks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active ringing alarm and focus mode states
    val activeRingingAlarm: StateFlow<Alarm?> = AlarmStateHolder.ringingAlarm
    val focusModeState: StateFlow<FocusState?> = AlarmStateHolder.focusModeState

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _verificationError = MutableStateFlow<String?>(null)
    val verificationError: StateFlow<String?> = _verificationError.asStateFlow()

    private val _verificationSuccessMsg = MutableStateFlow<String?>(null)
    val verificationSuccessMsg: StateFlow<String?> = _verificationSuccessMsg.asStateFlow()

    fun triggerRinging(alarm: Alarm) {
        _verificationError.value = null
        _verificationSuccessMsg.value = null
        AlarmStateHolder.startRinging(alarm)
    }

    fun dismissRinging() {
        AlarmStateHolder.stopRinging()
    }

    fun startFocusMode(alarmId: Int, durationMinutes: Int, taskType: String, taskDetails: String) {
        AlarmStateHolder.startFocusMode(alarmId, durationMinutes, taskType, taskDetails)
    }

    fun clearFocusMode() {
        AlarmStateHolder.clearFocusMode()
    }

    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val id = alarmRepo.insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = id.toInt())
            if (savedAlarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), savedAlarm)
            }
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepo.updateAlarm(alarm)
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), alarm)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepo.deleteAlarm(alarm)
            AlarmScheduler.cancelAlarm(getApplication(), alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmRepo.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), updated)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            proofRepo.clearLogs()
        }
    }

    fun verifyProofPhoto(
        alarm: Alarm,
        base64Photo: String,
        gpsLat: Double?,
        gpsLng: Double?,
        photoPath: String?
    ) {
        viewModelScope.launch {
            _isVerifying.value = true
            _verificationError.value = null
            _verificationSuccessMsg.value = null

            val base64ReferencePhoto = if (alarm.referencePhotoPath != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val file = java.io.File(alarm.referencePhotoPath)
                        if (file.exists()) {
                            val bytes = file.readBytes()
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                null
            }

            val result = com.example.data.api.GeminiClient.verifyPhotoProof(
                base64Photo = base64Photo,
                taskType = alarm.taskType,
                taskDetails = alarm.taskDetails,
                base64ReferencePhoto = base64ReferencePhoto
            )

            val status = if (result.verified) "PASSED" else "FAILED"

            // Save in DB and update streaks
            proofRepo.logProofAndUpdateStreak(
                alarmId = alarm.id,
                taskType = alarm.taskType,
                taskDetails = alarm.taskDetails,
                status = status,
                reviewNotes = result.reason,
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                confidence = result.confidence,
                photoPath = photoPath
            )

            _isVerifying.value = false
            if (result.verified) {
                _verificationSuccessMsg.value = result.reason
                AlarmStateHolder.stopRinging()
                AlarmStateHolder.clearFocusMode()
            } else {
                _verificationError.value = result.reason
            }
        }
    }
}
