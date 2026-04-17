package org.duhen.dglyphs;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VolumeGlyphService extends Service {

    private static final int LINE_SEGMENTS = 8;
    private static final long DISPLAY_DURATION_MS = 2000;
    private static final int FADE_STEP_MS = 20;
    private static final int FADE_DECREMENT = 150;

    private SharedPreferences prefs;
    private AudioManager audioManager;
    private PowerManager powerManager;
    private ExecutorService executor;
    private Future<?> animationFuture;

    private volatile int pendingVolume = -1;
    private volatile int pendingMax = 1;

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction()) ||
                    intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != AudioManager.STREAM_MUSIC) {
                return;
            }

            if (!powerManager.isInteractive() ||
                    !prefs.getBoolean("master_allow", false) ||
                    !prefs.getBoolean("volume_glyph_enabled", false) ||
                    SleepGuard.isBlocked(prefs)) {
                return;
            }

            int current = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
            if (current < 0) return;

            pendingVolume = current;
            pendingMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            if (animationFuture == null || animationFuture.isDone()) {
                animationFuture = executor.submit(() -> {
                    try {
                        runAnimation();
                    } finally {
                        GlyphManager.resetFrame();
                    }
                });
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        executor = Executors.newSingleThreadExecutor();
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void runAnimation() {
        int brightness = prefs.getInt("brightness", 2048);
        int[] frame = new int[15];
        int lastSegments = -1;
        long deadline = SystemClock.elapsedRealtime() + DISPLAY_DURATION_MS;

        while (SystemClock.elapsedRealtime() < deadline) {
            if (Thread.currentThread().isInterrupted()) return;

            int current = pendingVolume, max = pendingMax;
            int segments = (max > 0) ? Math.round((float) current / max * LINE_SEGMENTS) : 0;

            if (segments != lastSegments) {
                lastSegments = segments;
                int dotBrightness = (current > 0) ? brightness : 0;
                frame[6] = dotBrightness;

                for (int s = 0; s < LINE_SEGMENTS; s++) {
                    frame[7 + s] = (s < segments) ? brightness : 0;
                }
                GlyphManager.setFrame(frame);
                GlyphManager.setBrightness(GlyphManager.Glyph.DOT, dotBrightness);
                deadline = SystemClock.elapsedRealtime() + DISPLAY_DURATION_MS;
            }
            SystemClock.sleep(50);
        }

        for (int b = brightness; b >= 0; b -= FADE_DECREMENT) {
            if (Thread.currentThread().isInterrupted()) return;
            GlyphManager.setBrightness(GlyphManager.Glyph.DOT, b);

            for (int i = 7; i < 15; i++) {
                if (frame[i] > 0) frame[i] = b;
            }
            GlyphManager.setFrame(frame);
            SystemClock.sleep(FADE_STEP_MS);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(volumeReceiver);
        if (animationFuture != null) animationFuture.cancel(true);
        GlyphManager.resetFrame();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}