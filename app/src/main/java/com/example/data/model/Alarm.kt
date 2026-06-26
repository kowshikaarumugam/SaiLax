package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val taskType: String, // "Study", "Gym", "Reading", "Custom"
    val taskDetails: String,
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun", // Comma separated
    val ringtoneUri: String? = null,
    val isVibrate: Boolean = true,
    val label: String = "",
    val ringDurationMinutes: Int = 5,
    val activityDurationMinutes: Int = 30,
    val referencePhotoPath: String? = null
)
