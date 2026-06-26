package com.example.data.firebase;

import android.content.Context;
import android.util.Log;
import com.example.data.model.Alarm;
import com.example.data.model.HabitStreak;
import com.example.data.model.ProofLog;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final MutableStateFlow<Boolean> _isFirebaseConnected = StateFlowKt.MutableStateFlow(false);
    public static final StateFlow<Boolean> isFirebaseConnected = _isFirebaseConnected;

    private static final MutableStateFlow<String> _syncStatus = StateFlowKt.MutableStateFlow("Local-only mode (Awaiting connection)");
    public static final StateFlow<String> syncStatus = _syncStatus;

    static {
        checkFirebaseAvailability();
    }

    public static boolean isFirebaseAvailable() {
        try {
            FirebaseApp.getInstance();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static void checkFirebaseAvailability() {
        boolean available = isFirebaseAvailable();
        _isFirebaseConnected.setValue(available);
        _syncStatus.setValue(available ? 
            "Cloud Sync Ready (Firebase Active)" : 
            "Local Mode (Firebase options missing. Running securely on local Room DB)"
        );
        Log.d(TAG, "Firebase Availability checked: " + available);
    }

    public static Task<Object> syncDataToFirestore(
        Context context,
        List<Alarm> alarms,
        List<HabitStreak> streaks,
        List<ProofLog> logs
    ) {
        if (!isFirebaseAvailable()) {
            _syncStatus.setValue("Local DB Active (Room Persistence)");
            return Tasks.forResult(null);
        }

        _syncStatus.setValue("Syncing with Firestore...");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            _syncStatus.setValue("Cloud Sync Paused: Log in to sync");
            return Tasks.forResult(null);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        return Tasks.call(executor, () -> {
            // Sync Alarms
            CollectionReference alarmsCollection = db.collection("users").document(userId).collection("alarms");
            for (Alarm alarm : alarms) {
                Map<String, Object> alarmMap = new HashMap<>();
                alarmMap.put("id", alarm.getId());
                alarmMap.put("hour", alarm.getHour());
                alarmMap.put("minute", alarm.getMinute());
                alarmMap.put("label", alarm.getLabel());
                alarmMap.put("isEnabled", alarm.isEnabled());
                alarmMap.put("taskType", alarm.getTaskType());
                alarmMap.put("taskDetails", alarm.getTaskDetails());
                alarmMap.put("activityDurationMinutes", alarm.getActivityDurationMinutes());
                alarmMap.put("referencePhotoPath", alarm.getReferencePhotoPath());
                alarmMap.put("daysOfWeek", alarm.getDaysOfWeek());
                alarmMap.put("isVibrate", alarm.isVibrate());
                alarmMap.put("ringDurationMinutes", alarm.getRingDurationMinutes());
                alarmMap.put("ringtoneUri", alarm.getRingtoneUri());

                Tasks.await(alarmsCollection.document(String.valueOf(alarm.getId())).set(alarmMap));
            }

            // Sync Streaks
            CollectionReference streaksCollection = db.collection("users").document(userId).collection("streaks");
            for (HabitStreak streak : streaks) {
                Map<String, Object> streakMap = new HashMap<>();
                streakMap.put("taskType", streak.getTaskType());
                streakMap.put("currentStreak", streak.getCurrentStreak());
                streakMap.put("longestStreak", streak.getLongestStreak());
                streakMap.put("lastSuccessDate", streak.getLastSuccessDate());

                Tasks.await(streaksCollection.document(streak.getTaskType()).set(streakMap));
            }

            // Sync Logs
            CollectionReference logsCollection = db.collection("users").document(userId).collection("logs");
            for (ProofLog log : logs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("alarmId", log.getAlarmId());
                logMap.put("taskType", log.getTaskType());
                logMap.put("taskDetails", log.getTaskDetails());
                logMap.put("timestamp", log.getTimestamp());
                logMap.put("status", log.getStatus());
                logMap.put("reviewNotes", log.getReviewNotes());
                logMap.put("gpsLatitude", log.getGpsLatitude());
                logMap.put("gpsLongitude", log.getGpsLongitude());
                logMap.put("confidence", log.getConfidence());
                logMap.put("photoPath", log.getPhotoPath());

                Tasks.await(logsCollection.document(String.valueOf(log.getId())).set(logMap));
            }

            _syncStatus.setValue("Cloud Sync Success: All data secure!");
            return null;
        }).addOnFailureListener(e -> {
            _syncStatus.setValue("Cloud Sync Failed: " + e.getLocalizedMessage());
            Log.e(TAG, "Error syncing to Firestore", e);
        });
    }
}
