package org.duhen.dglyphs;

import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratorUtils {
    private static final int MIN_INTERVAL = 50;
    private static long lastTickTime = 0;

    public static void quickTick(Vibrator vibrator, int duration, int amplitude) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTickTime < MIN_INTERVAL) {
            return;
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
            lastTickTime = currentTime;
        }
    }
}