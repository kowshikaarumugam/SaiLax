package com.example.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms")
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    private int id = 0;
    private int hour;
    private int minute;
    private String taskType; // "Study", "Gym", "Reading", "Custom"
    private String taskDetails;
    private boolean isEnabled = true;
    private String daysOfWeek = "Mon,Tue,Wed,Thu,Fri,Sat,Sun"; // Comma separated
    private String ringtoneUri = null;
    private boolean isVibrate = true;
    private String label = "";
    private int ringDurationMinutes = 5;
    private int activityDurationMinutes = 30;
    private String referencePhotoPath = null;

    // Default Constructor for Room
    public Alarm() {
    }

    // Full Constructor
    public Alarm(int id, int hour, int minute, String taskType, String taskDetails, 
                 boolean isEnabled, String daysOfWeek, String ringtoneUri, 
                 boolean isVibrate, String label, int ringDurationMinutes, 
                 int activityDurationMinutes, String referencePhotoPath) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.taskType = taskType;
        this.taskDetails = taskDetails;
        this.isEnabled = isEnabled;
        this.daysOfWeek = daysOfWeek;
        this.ringtoneUri = ringtoneUri;
        this.isVibrate = isVibrate;
        this.label = label;
        this.ringDurationMinutes = ringDurationMinutes;
        this.activityDurationMinutes = activityDurationMinutes;
        this.referencePhotoPath = referencePhotoPath;
    }

    // Helper Copy Methods for Kotlin compatibility
    public Alarm copyWithId(int id) {
        return new Alarm(id, this.hour, this.minute, this.taskType, this.taskDetails, 
                         this.isEnabled, this.daysOfWeek, this.ringtoneUri, 
                         this.isVibrate, this.label, this.ringDurationMinutes, 
                         this.activityDurationMinutes, this.referencePhotoPath);
    }

    public Alarm copyWithIsEnabled(boolean isEnabled) {
        return new Alarm(this.id, this.hour, this.minute, this.taskType, this.taskDetails, 
                         isEnabled, this.daysOfWeek, this.ringtoneUri, 
                         this.isVibrate, this.label, this.ringDurationMinutes, 
                         this.activityDurationMinutes, this.referencePhotoPath);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(String taskDetails) {
        this.taskDetails = taskDetails;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public String getRingtoneUri() {
        return ringtoneUri;
    }

    public void setRingtoneUri(String ringtoneUri) {
        this.ringtoneUri = ringtoneUri;
    }

    public boolean isVibrate() {
        return isVibrate;
    }

    public void setVibrate(boolean vibrate) {
        isVibrate = vibrate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getRingDurationMinutes() {
        return ringDurationMinutes;
    }

    public void setRingDurationMinutes(int ringDurationMinutes) {
        this.ringDurationMinutes = ringDurationMinutes;
    }

    public int getActivityDurationMinutes() {
        return activityDurationMinutes;
    }

    public void setActivityDurationMinutes(int activityDurationMinutes) {
        this.activityDurationMinutes = activityDurationMinutes;
    }

    public String getReferencePhotoPath() {
        return referencePhotoPath;
    }

    public void setReferencePhotoPath(String referencePhotoPath) {
        this.referencePhotoPath = referencePhotoPath;
    }
}
