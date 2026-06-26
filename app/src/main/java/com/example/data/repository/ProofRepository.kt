package com.example.data.repository

import com.example.data.dao.ProofLogDao
import com.example.data.dao.HabitStreakDao
import com.example.data.model.ProofLog
import com.example.data.model.HabitStreak
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class ProofRepository(
    private val proofLogDao: ProofLogDao,
    private val habitStreakDao: HabitStreakDao
) {
    val allLogs: Flow<List<ProofLog>> = proofLogDao.getAllLogs()
    val allStreaks: Flow<List<HabitStreak>> = habitStreakDao.getAllStreaks()

    suspend fun clearLogs() = proofLogDao.clearLogs()

    suspend fun logProofAndUpdateStreak(
        alarmId: Int,
        taskType: String,
        taskDetails: String,
        status: String,
        reviewNotes: String,
        gpsLat: Double?,
        gpsLng: Double?,
        confidence: Double,
        photoPath: String?
    ) {
        val log = ProofLog(
            alarmId = alarmId,
            timestamp = System.currentTimeMillis(),
            taskType = taskType,
            taskDetails = taskDetails,
            status = status,
            reviewNotes = reviewNotes,
            gpsLatitude = gpsLat,
            gpsLongitude = gpsLng,
            confidence = confidence,
            photoPath = photoPath
        )
        proofLogDao.insertLog(log)

        if (status == "PASSED") {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val currentStreakObj = habitStreakDao.getStreakByTaskType(taskType)
            if (currentStreakObj == null) {
                habitStreakDao.insertOrUpdateStreak(
                    HabitStreak(
                        taskType = taskType,
                        currentStreak = 1,
                        longestStreak = 1,
                        lastSuccessDate = todayStr
                    )
                )
            } else {
                val lastDateStr = currentStreakObj.lastSuccessDate
                if (lastDateStr == todayStr) {
                    // Already succeeded today, keep current streak
                } else {
                    val isYesterday = isYesterday(lastDateStr)
                    val newStreak = if (isYesterday) currentStreakObj.currentStreak + 1 else 1
                    val newLongest = maxOf(newStreak, currentStreakObj.longestStreak)
                    habitStreakDao.insertOrUpdateStreak(
                        HabitStreak(
                            taskType = taskType,
                            currentStreak = newStreak,
                            longestStreak = newLongest,
                            lastSuccessDate = todayStr
                        )
                    )
                }
            }
        }
    }

    private fun isYesterday(dateStr: String?): Boolean {
        if (dateStr == null) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStrClean = dateStr.trim()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            dateStrClean == yesterdayStr
        } catch (e: Exception) {
            false
        }
    }
}
