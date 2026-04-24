package org.duhen.dglyphs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GlyphEffects {

    public static final String FOLDER_CALL = "call";
    public static final String FOLDER_NOTIF = "notification";

    private static final int FRAME_DURATION = 20;
    private static final GlyphManager.Glyph[] GLYPH_ORDER = {
            GlyphManager.Glyph.CAMERA,
            GlyphManager.Glyph.DIAGONAL,
            GlyphManager.Glyph.MAIN,
            GlyphManager.Glyph.LINE,
            GlyphManager.Glyph.DOT
    };
    private static volatile Thread currentThread = null;

    public static void playFromPref(Context context, String prefKey, String defaultFolder,
                                    Vibrator vibrator, int brightness) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getString(R.string.pref_file), Context.MODE_PRIVATE);
        String val = prefs.getString(prefKey, null);
        String folder = defaultFolder;
        String fileName = null;
        if (val != null && val.contains("/")) {
            int idx = val.lastIndexOf('/');
            folder = val.substring(0, idx);
            fileName = val.substring(idx + 1);
        } else if (val != null && !val.isEmpty()) {
            fileName = val;
        }
        if (fileName == null || fileName.isEmpty()) {
            try {
                String[] files = context.getAssets().list(folder);
                if (files != null) for (String f : files)
                    if (f.endsWith(".csv")) {
                        fileName = f.replace(".csv", "");
                        break;
                    }
            } catch (Exception ignored) {
            }
        }
        if (fileName == null || fileName.isEmpty()) return;
        play(context, folder, fileName, vibrator, brightness, vibKeyForPref(prefKey));
    }

    private static void play(Context context, String folder, String fileName,
                             Vibrator vibrator, int brightness, String vibKey) {
        stop();
        Context appCtx = context.getApplicationContext();
        float scale = (float) brightness / GlyphManager.MAX_BRIGHTNESS;

        SharedPreferences prefs = appCtx.getSharedPreferences(
                appCtx.getString(R.string.pref_file), Context.MODE_PRIVATE);
        int step = VibratorUtils.readStep(prefs, vibKey);
        int vibAmp = VibratorUtils.amplitudeForStep(step);
        int vibDur = VibratorUtils.durationForStep(step);

        currentThread = new Thread(() -> {
            try (InputStream is = appCtx.getAssets().open(folder + "/" + fileName + ".csv");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                boolean vibratedThisCycle = false;
                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    String[] vals = trimmed.split("[,\\t ]+");
                    if (vals.length < GLYPH_ORDER.length) continue;
                    try {
                        int[] bright = new int[GLYPH_ORDER.length];
                        boolean anyNonZero = false;
                        for (int i = 0; i < GLYPH_ORDER.length; i++) {
                            bright[i] = Math.round(Integer.parseInt(vals[i]) * scale);
                            if (bright[i] > 0) anyNonZero = true;
                        }
                        if (anyNonZero && !vibratedThisCycle) {
                            VibratorUtils.vibrate(vibrator, vibDur, vibAmp);
                            vibratedThisCycle = true;
                        }
                        if (!anyNonZero) vibratedThisCycle = false;
                        for (int i = 0; i < GLYPH_ORDER.length; i++) {
                            GlyphManager.setBrightness(GLYPH_ORDER[i], bright[i]);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    try {
                        Thread.sleep(FRAME_DURATION);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                GlyphManager.resetFrame();
            }
        });
        currentThread.start();
    }

    public static void stop() {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
            try {
                currentThread.join(100);
            } catch (InterruptedException ignored) {
            }
        }
        GlyphManager.resetFrame();
    }

    private static String vibKeyForPref(String prefKey) {
        switch (prefKey) {
            case "call_style":
                return VibratorUtils.KEY_VIB_CALL;
            case "flip_style":
                return VibratorUtils.KEY_VIB_FLIP;
            default:
                return VibratorUtils.KEY_VIB_NOTIF;
        }
    }

    private static String vibKeyForFolder(String folder) {
        return FOLDER_CALL.equals(folder) ? VibratorUtils.KEY_VIB_CALL : VibratorUtils.KEY_VIB_NOTIF;
    }
}