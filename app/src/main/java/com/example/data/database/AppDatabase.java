package com.example.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.data.dao.AlarmDao;
import com.example.data.dao.HabitStreakDao;
import com.example.data.dao.ProofLogDao;
import com.example.data.model.Alarm;
import com.example.data.model.HabitStreak;
import com.example.data.model.ProofLog;

@Database(entities = {Alarm.class, ProofLog.class, HabitStreak.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AlarmDao alarmDao();
    public abstract ProofLogDao proofLogDao();
    public abstract HabitStreakDao habitStreakDao();

    private static volatile AppDatabase INSTANCE = null;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "proof_alarm_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
