package com.example.data.dao

import androidx.room.*
import com.example.data.model.HabitStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitStreakDao {
    @Query("SELECT * FROM habit_streaks")
    fun getAllStreaks(): Flow<List<HabitStreak>>

    @Query("SELECT * FROM habit_streaks WHERE taskType = :taskType")
    suspend fun getStreakByTaskType(taskType: String): HabitStreak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: HabitStreak)
}
