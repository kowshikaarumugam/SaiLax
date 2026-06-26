package com.example.data.repository;

import com.example.data.dao.AlarmDao;
import com.example.data.model.Alarm;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

public class AlarmRepository {
    private final AlarmDao alarmDao;
    public final Flow<List<Alarm>> allAlarms;

    public AlarmRepository(AlarmDao alarmDao) {
        this.alarmDao = alarmDao;
        this.allAlarms = alarmDao.getAllAlarms();
    }

    public Flow<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public Alarm getAlarmById(int id) {
        return alarmDao.getAlarmById(id);
    }

    public long insertAlarm(Alarm alarm) {
        return alarmDao.insertAlarm(alarm);
    }

    public void updateAlarm(Alarm alarm) {
        alarmDao.updateAlarm(alarm);
    }

    public void deleteAlarm(Alarm alarm) {
        alarmDao.deleteAlarm(alarm);
    }
}
