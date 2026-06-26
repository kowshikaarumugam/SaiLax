package com.example.ui.alarm;

import com.example.data.model.Alarm;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

public class AlarmStateHolder {
    private static final MutableStateFlow<Alarm> _ringingAlarm = StateFlowKt.MutableStateFlow(null);
    public static final StateFlow<Alarm> ringingAlarm = _ringingAlarm;

    private static final MutableStateFlow<FocusState> _focusModeState = StateFlowKt.MutableStateFlow(null);
    public static final StateFlow<FocusState> focusModeState = _focusModeState;

    public static void startRinging(Alarm alarm) {
        _ringingAlarm.setValue(alarm);
    }

    public static void stopRinging() {
        _ringingAlarm.setValue(null);
    }

    public static void startFocusMode(
        int alarmId,
        int durationMinutes,
        String taskType,
        String taskDetails,
        String referencePhotoPath
    ) {
        long totalMillis = durationMinutes * 60 * 1000L;
        long endTime = System.currentTimeMillis() + totalMillis;
        _focusModeState.setValue(new FocusState(
            alarmId,
            endTime,
            Math.max(totalMillis, 1000L),
            taskType,
            taskDetails,
            referencePhotoPath
        ));
    }

    public static void clearFocusMode() {
        _focusModeState.setValue(null);
    }
}
