package com.example.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_streaks")
public class HabitStreak {
    @PrimaryKey
    @NonNull
    private String taskType; // "Study", "Gym", "Reading", "Custom"
    private int currentStreak = 0;
    private int longestStreak = 0;
    private String lastSuccessDate = null; // YYYY-MM-DD

    // Default Constructor for Room
    public HabitStreak() {
    }

    // Full Constructor
    public HabitStreak(@NonNull String taskType, int currentStreak, int longestStreak, String lastSuccessDate) {
        this.taskType = taskType;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastSuccessDate = lastSuccessDate;
    }

    // 1-Parameter Constructor
    public HabitStreak(@NonNull String taskType) {
        this.taskType = taskType;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.lastSuccessDate = null;
    }

    // Getters and Setters
    @NonNull
    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(@NonNull String taskType) {
        this.taskType = taskType;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public String getLastSuccessDate() {
        return lastSuccessDate;
    }

    public void setLastSuccessDate(String lastSuccessDate) {
        this.lastSuccessDate = lastSuccessDate;
    }
}
