package com.example.ui.alarm;

public class FocusState {
    private final int alarmId;
    private final long endTimeMillis;
    private final long totalDurationMillis;
    private final String taskType;
    private final String taskDetails;
    private final String referencePhotoPath;

    public FocusState(int alarmId, long endTimeMillis, long totalDurationMillis, String taskType, String taskDetails, String referencePhotoPath) {
        this.alarmId = alarmId;
        this.endTimeMillis = endTimeMillis;
        this.totalDurationMillis = totalDurationMillis;
        this.taskType = taskType;
        this.taskDetails = taskDetails;
        this.referencePhotoPath = referencePhotoPath;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTaskDetails() {
        return taskDetails;
    }

    public String getReferencePhotoPath() {
        return referencePhotoPath;
    }
}
