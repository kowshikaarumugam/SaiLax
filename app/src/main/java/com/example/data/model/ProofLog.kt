package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proof_logs")
data class ProofLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alarmId: Int,
    val timestamp: Long,
    val taskType: String,
    val taskDetails: String,
    val status: String, // "PASSED", "FAILED"
    val reviewNotes: String, // Reason from Gemini AI
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val confidence: Double = 1.0,
    val photoPath: String? = null
)
