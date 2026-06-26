package com.example.ui.alarm

import com.example.data.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlarmStateHolder {
    private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
    val ringingAlarm: StateFlow<Alarm?> = _ringingAlarm.asStateFlow()

    private val _focusModeState = MutableStateFlow<FocusState?>(null)
    val focusModeState: StateFlow<FocusState?> = _focusModeState.asStateFlow()

    fun startRinging(alarm: Alarm) {
        _ringingAlarm.value = alarm
    }

    fun stopRinging() {
        _ringingAlarm.value = null
    }

    fun startFocusMode(
        alarmId: Int,
        durationMinutes: Int,
        taskType: String,
        taskDetails: String,
        referencePhotoPath: String? = null
    ) {
        val totalMillis = durationMinutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + totalMillis
        _focusModeState.value = FocusState(
            alarmId = alarmId,
            endTimeMillis = endTime,
            totalDurationMillis = totalMillis.coerceAtLeast(1000L),
            taskType = taskType,
            taskDetails = taskDetails,
            referencePhotoPath = referencePhotoPath
        )
    }

    fun clearFocusMode() {
        _focusModeState.value = null
    }
}

data class FocusState(
    val alarmId: Int,
    val endTimeMillis: Long,
    val totalDurationMillis: Long,
    val taskType: String,
    val taskDetails: String,
    val referencePhotoPath: String? = null
)
