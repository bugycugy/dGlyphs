package org.duhen.dglyphs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PreviewAnimator {

    private static final int FRAME_MS = 20;
    private static final int GLYPH_COUNT = 5;
    private static final int PLAY_DELAY_MS = 500;

    private static final GlyphManager.Glyph[] GLYPH_ORDER = {
            GlyphManager.Glyph.CAMERA,
            GlyphManager.Glyph.DIAGONAL,
            GlyphManager.Glyph.MAIN,
            GlyphManager.Glyph.LINE,
            GlyphManager.Glyph.DOT
    };

    private final Context appCtx;
    private final GlyphPreviewView previewView;
    private final Vibrator vibrator;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final String vibKey;

    private volatile Thread thread;
    private Runnable pendingPlay;

    public PreviewAnimator(Context context, GlyphPreviewView previewView, String vibKey) {
        this.appCtx = context.getApplicationContext();
        this.previewView = previewView;
        this.vibKey = vibKey;
        VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        this.vibrator = vm != null ? vm.getDefaultVibrator() : null;
    }

    public void play(String folder, String fileName) {
        if (pendingPlay != null) uiHandler.removeCallbacks(pendingPlay);
        stopThread();
        pendingPlay = () -> {
            pendingPlay = null;
            startThread(folder, fileName);
        };
        uiHandler.postDelayed(pendingPlay, PLAY_DELAY_MS);
    }

    public void stop() {
        if (pendingPlay != null) {
            uiHandler.removeCallbacks(pendingPlay);
            pendingPlay = null;
        }
        stopThread();
        uiHandler.post(previewView::resetAll);
    }

    private void startThread(String folder, String fileName) {
        SharedPreferences prefs = appCtx.getSharedPreferences(
                appCtx.getString(R.string.pref_file), Context.MODE_PRIVATE);

        int vibStep = VibratorUtils.readStep(prefs, vibKey);
        int vibAmp = VibratorUtils.amplitudeForStep(vibStep);
        int vibDur = VibratorUtils.durationForStep(vibStep);

        thread = new Thread(() -> {
            try (InputStream is = appCtx.getAssets().open(folder + "/" + fileName + ".csv");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                boolean vibratedThisCycle = false;
                String line;

                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    String[] parts = trimmed.split("[,\\t ]+");
                    if (parts.length < GLYPH_COUNT) continue;

                    int[] bright = new int[GLYPH_COUNT];
                    try {
                        for (int i = 0; i < GLYPH_COUNT; i++)
                            bright[i] = Integer.parseInt(parts[i]);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }

                    boolean anyNonZero = false;
                    for (int b : bright)
                        if (b > 0) {
                            anyNonZero = true;
                            break;
                        }

                    float scale = prefs.getInt("brightness", 2048) / (float) GlyphManager.MAX_BRIGHTNESS;
                    for (int i = 0; i < GLYPH_COUNT; i++) {
                        GlyphManager.setBrightness(GLYPH_ORDER[i], Math.round(bright[i] * scale));
                    }

                    if (anyNonZero && !vibratedThisCycle) {
                        VibratorUtils.vibrate(vibrator, vibDur, vibAmp);
                        vibratedThisCycle = true;
                    }
                    if (!anyNonZero) vibratedThisCycle = false;

                    uiHandler.post(() -> previewView.setGlyphState(bright));

                    try {
                        Thread.sleep(FRAME_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                uiHandler.post(previewView::resetAll);
                GlyphManager.resetFrame();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void stopThread() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException ignored) {
            }
        }
        thread = null;
        GlyphManager.resetFrame();
    }
}