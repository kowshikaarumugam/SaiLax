package com.example.ui.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.MainActivity;
import com.example.data.database.AppDatabase;
import com.example.data.model.Alarm;
import java.util.concurrent.Executors;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private static final int NOTIFICATION_ID = 9999;
    private static final String CHANNEL_ID = "OWNUP_ALARM_CHANNEL";

    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;
    private Alarm currentAlarm = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoStopRunnable = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AlarmService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int alarmId = intent != null ? intent.getIntExtra("ALARM_ID", -1) : -1;
        Log.d(TAG, "onStartCommand with Alarm ID: " + alarmId);

        if (alarmId != -1) {
            startForegroundServiceFlow(alarmId);
        } else {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundServiceFlow(int alarmId) {
        // First, build a quick placeholder notification so startForeground doesn't fail on newer Android versions
        createNotificationChannel();
        Notification notification = buildNotification("Morning Wake Up Alarm", "Time to own your morning!", null);
        startForeground(NOTIFICATION_ID, notification);

        // Fetch alarm on a background thread and then post back to the main thread
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            Alarm alarm = db.alarmDao().getAlarmById(alarmId);

            handler.post(() -> {
                if (alarm != null) {
                    currentAlarm = alarm;
                    Log.d(TAG, "Found alarm: " + alarm.getTaskType());

                    // Update global alarm state holder
                    AlarmStateHolder.startRinging(alarm);

                    // Update notification with real details
                    Notification updatedNotification = buildNotification(
                        "ALARM RINGING: " + alarm.getTaskType(),
                        alarm.getTaskDetails(),
                        alarm
                    );
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.notify(NOTIFICATION_ID, updatedNotification);
                    }

                    // Play Audio and Vibrate
                    startAudio(alarm);
                    startVibration(alarm);

                    // Start automatic ring duration timeout
                    scheduleAutoStop(alarm);
                } else {
                    Log.e(TAG, "Alarm ID " + alarmId + " not found in database");
                    stopSelf();
                }
            });
        });
    }

    private void scheduleAutoStop(final Alarm alarm) {
        cancelAutoStop();

        long durationMs = alarm.getRingDurationMinutes() * 60 * 1000L;
        autoStopRunnable = () -> {
            Log.d(TAG, "Auto-stop duration reached. Moving to Focus Mode!");
            
            // Auto transition to Focus Mode even if user missed it
            AlarmStateHolder.startFocusMode(
                alarm.getId(),
                alarm.getActivityDurationMinutes(),
                alarm.getTaskType(),
                alarm.getTaskDetails(),
                alarm.getReferencePhotoPath()
            );
            AlarmStateHolder.stopRinging();
            stopSelf();
        };

        handler.postDelayed(autoStopRunnable, durationMs);
    }

    private void cancelAutoStop() {
        if (autoStopRunnable != null) {
            handler.removeCallbacks(autoStopRunnable);
            autoStopRunnable = null;
        }
    }

    private void startAudio(Alarm alarm) {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), ringtoneUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                );
            } else {
                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to play audio", e);
        }
    }

    private void startVibration(Alarm alarm) {
        if (!alarm.isVibrate()) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator == null) return;

            long[] pattern = {0, 800, 400, 800};
            int[] amplitudes = {0, 255, 0, 255};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start vibration", e);
        }
    }

    private Notification buildNotification(String title, String text, Alarm alarm) {
        Intent fullScreenIntent = new Intent(getApplicationContext(), MainActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fullScreenIntent.putExtra("LAUNCH_RINGING_ALARM", true);
        if (alarm != null) {
            fullScreenIntent.putExtra("ALARM_ID", alarm.getId());
        }

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            getApplicationContext(),
            0,
            fullScreenIntent,
            pendingIntentFlags
        );

        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "OwnUp Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channels for ringing wake-up alarms in OwnUp");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setBypassDnd(true); // Crucial to ring in Silent/DND mode!

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlarmService destroyed");
        cancelAutoStop();

        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up MediaPlayer", e);
        }

        try {
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up Vibrator", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
