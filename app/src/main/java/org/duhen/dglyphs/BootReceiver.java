package org.duhen.dglyphs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(
                context.getString(R.string.pref_file), Context.MODE_PRIVATE);

        if (!prefs.getBoolean("master_allow", false)) return;

        if (prefs.getBoolean("flip_enabled", false)) {
            context.startService(new Intent(context, FlipToGlyphService.class));
        }
        if (prefs.getBoolean("battery_glyph_enabled", false)) {
            context.startService(new Intent(context, BatteryGlyphService.class));
        }
        if (prefs.getBoolean("volume_glyph_enabled", false)) {
            context.startService(new Intent(context, VolumeGlyphService.class));
        }
    }
}