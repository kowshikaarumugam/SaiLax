package com.example.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.data.database.AppDatabase;
import com.example.data.model.Alarm;
import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Phone rebooted! Rescheduling active alarms...");

            final PendingResult pendingResult = goAsync();
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(context);
                    List<Alarm> enabledAlarms = db.alarmDao().getEnabledAlarms();

                    for (Alarm alarm : enabledAlarms) {
                        AlarmScheduler.scheduleAlarm(context, alarm);
                    }
                    Log.d(TAG, "Rescheduled " + enabledAlarms.size() + " alarms.");
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling alarms: ", e);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
