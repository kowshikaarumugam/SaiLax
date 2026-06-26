package com.example.data.dao;

import androidx.room.*;
import com.example.data.model.HabitStreak;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface HabitStreakDao {
    @Query("SELECT * FROM habit_streaks")
    Flow<List<HabitStreak>> getAllStreaks();

    @Query("SELECT * FROM habit_streaks WHERE taskType = :taskType")
    HabitStreak getStreakByTaskType(String taskType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateStreak(HabitStreak streak);
}
