package com.example.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "proof_logs")
public class ProofLog {
    @PrimaryKey(autoGenerate = true)
    private int id = 0;
    private int alarmId;
    private long timestamp;
    private String taskType;
    private String taskDetails;
    private String status; // "PASSED", "FAILED"
    private String reviewNotes; // Reason from Gemini AI
    private Double gpsLatitude = null;
    private Double gpsLongitude = null;
    private double confidence = 1.0;
    private String photoPath = null;

    // Default Constructor for Room
    public ProofLog() {
    }

    // Full Constructor
    public ProofLog(int id, int alarmId, long timestamp, String taskType, String taskDetails, 
                    String status, String reviewNotes, Double gpsLatitude, Double gpsLongitude, 
                    double confidence, String photoPath) {
        this.id = id;
        this.alarmId = alarmId;
        this.timestamp = timestamp;
        this.taskType = taskType;
        this.taskDetails = taskDetails;
        this.status = status;
        this.reviewNotes = reviewNotes;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.confidence = confidence;
        this.photoPath = photoPath;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Double getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(Double gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public Double getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(Double gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
