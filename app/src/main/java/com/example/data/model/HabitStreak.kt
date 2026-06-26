package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_streaks")
data class HabitStreak(
    @PrimaryKey val taskType: String, // "Study", "Gym", "Reading", "Custom"
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSuccessDate: String? = null // YYYY-MM-DD
)
