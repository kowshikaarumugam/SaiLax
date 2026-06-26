package com.example.data.dao;

import androidx.room.*;
import com.example.data.model.Alarm;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    Flow<List<Alarm>> getAllAlarms();

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    List<Alarm> getEnabledAlarms();

    @Query("SELECT * FROM alarms WHERE id = :id")
    Alarm getAlarmById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertAlarm(Alarm alarm);

    @Update
    void updateAlarm(Alarm alarm);

    @Delete
    void deleteAlarm(Alarm alarm);
}
