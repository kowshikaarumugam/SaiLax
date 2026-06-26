package com.example.data.repository;

import com.example.data.dao.ProofLogDao;
import com.example.data.dao.HabitStreakDao;
import com.example.data.model.ProofLog;
import com.example.data.model.HabitStreak;
import kotlinx.coroutines.flow.Flow;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProofRepository {
    private final ProofLogDao proofLogDao;
    private final HabitStreakDao habitStreakDao;
    
    public final Flow<List<ProofLog>> allLogs;
    public final Flow<List<HabitStreak>> allStreaks;

    public ProofRepository(ProofLogDao proofLogDao, HabitStreakDao habitStreakDao) {
        this.proofLogDao = proofLogDao;
        this.habitStreakDao = habitStreakDao;
        this.allLogs = proofLogDao.getAllLogs();
        this.allStreaks = habitStreakDao.getAllStreaks();
    }

    public Flow<List<ProofLog>> getAllLogs() {
        return allLogs;
    }

    public Flow<List<HabitStreak>> getAllStreaks() {
        return allStreaks;
    }

    public void clearLogs() {
        proofLogDao.clearLogs();
    }

    public void logProofAndUpdateStreak(
        int alarmId,
        String taskType,
        String taskDetails,
        String status,
        String reviewNotes,
        Double gpsLat,
        Double gpsLng,
        double confidence,
        String photoPath
    ) {
        ProofLog log = new ProofLog(
            0,
            alarmId,
            System.currentTimeMillis(),
            taskType,
            taskDetails,
            status,
            reviewNotes,
            gpsLat,
            gpsLng,
            confidence,
            photoPath
        );
        proofLogDao.insertLog(log);

        if ("PASSED".equals(status)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayStr = sdf.format(new Date());

            HabitStreak currentStreakObj = habitStreakDao.getStreakByTaskType(taskType);
            if (currentStreakObj == null) {
                habitStreakDao.insertOrUpdateStreak(
                    new HabitStreak(
                        taskType,
                        1,
                        1,
                        todayStr
                    )
                );
            } else {
                String lastDateStr = currentStreakObj.getLastSuccessDate();
                if (todayStr.equals(lastDateStr)) {
                    // Already succeeded today, keep current streak
                } else {
                    boolean yesterday = isYesterday(lastDateStr);
                    int newStreak = yesterday ? currentStreakObj.getCurrentStreak() + 1 : 1;
                    int newLongest = Math.max(newStreak, currentStreakObj.getLongestStreak());
                    habitStreakDao.insertOrUpdateStreak(
                        new HabitStreak(
                            taskType,
                            newStreak,
                            newLongest,
                            todayStr
                        )
                    );
                }
            }
        }
    }

    private boolean isYesterday(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStrClean = dateStr.trim();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = sdf.format(cal.getTime());
            return dateStrClean.equals(yesterdayStr);
        } catch (Exception e) {
            return false;
        }
    }
}
