package org.duhen.dglyphs;

import android.content.SharedPreferences;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratorUtils {

    public static final String KEY_VIB_CALL = "vib_amplitude_call";
    public static final String KEY_VIB_NOTIF = "vib_amplitude_notif";
    public static final String KEY_VIB_FLIP = "vib_amplitude_flip";

    public static final int DEFAULT_VIB_STEP = 2;

    private static final int MIN_INTERVAL = 50;
    private static final int[] AMPLITUDES = {30, 80, 160, 255};
    private static final int[] DURATIONS = {20, 30, 50, 100};
    private static long lastTickTime = 0;

    public static int amplitudeForStep(int step) {
        return AMPLITUDES[clampIdx(step)];
    }

    public static int durationForStep(int step) {
        return DURATIONS[clampIdx(step)];
    }

    private static int clampIdx(int step) {
        return Math.max(0, Math.min(step - 1, 3));
    }

    public static void quickTick(Vibrator vibrator, int duration, int amplitude) {
        long now = System.currentTimeMillis();
        if (now - lastTickTime < MIN_INTERVAL) return;
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
            lastTickTime = now;
        }
    }

    public static void vibrate(Vibrator vibrator, int duration, int amplitude) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
        }
    }

    public static int readStep(SharedPreferences prefs, String modeKey) {
        return prefs.getInt(modeKey, DEFAULT_VIB_STEP);
    }
}