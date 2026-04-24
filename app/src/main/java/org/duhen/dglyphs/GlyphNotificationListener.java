package org.duhen.dglyphs;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class GlyphNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!sbn.isClearable()) return;
        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);
        if (!prefs.getBoolean("master_allow", false)) return;
        if (SleepGuard.isBlocked(prefs)) return;
        if (prefs.getBoolean("lockscreen_only", false) && GlyphManager.isUserActive(this)) return;
        GlyphEffects.playFromPref(this, "notif_style", GlyphEffects.FOLDER_NOTIF,
                getSystemService(android.os.Vibrator.class),
                prefs.getInt("brightness", 2048));
    }
}