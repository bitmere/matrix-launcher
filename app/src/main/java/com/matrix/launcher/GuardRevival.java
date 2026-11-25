package com.matrix.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.service.notification.NotificationListenerService;

/**
 * Starts this launcher's process after the firmware has killed it.
 *
 * A notification listener keeps its approval across a force stop, and the system binds it again
 * once the process is gone. That rebind is what brings Home back on TCL and Xiaomi. No notification
 * content is parsed. Media sessions are observed while connected. Enable with {@code cmd notification allow_listener com.matrix.launcher/com.gathin.tvhome.GuardRevival}.
 */
public class GuardRevival extends NotificationListenerService {
    private BroadcastReceiver unlocked;
    private AndroidNowPlaying nowPlaying;

    @Override
    public void onListenerConnected() {
        UserManager users = getSystemService(UserManager.class);
        if (users != null && !users.isUserUnlocked()) {
            if (unlocked != null) return;
            unlocked = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    stopWaiting();
                    GuardKeep.ensureHomeKey(GuardRevival.this);
                    startNowPlaying();
                }
            };
            registerReceiver(unlocked, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
            if (users.isUserUnlocked()) {
                stopWaiting();
                GuardKeep.ensureHomeKey(this);
                startNowPlaying();
            }
            return;
        }
        GuardKeep.ensureHomeKey(this);
        startNowPlaying();
    }

    private void startNowPlaying() {
        if (nowPlaying != null) nowPlaying.close();
        nowPlaying = new AndroidNowPlaying(getApplicationContext());
        nowPlaying.start();
    }

    private void stopWaiting() {
        if (unlocked == null) return;
        try {
            unregisterReceiver(unlocked);
        } catch (RuntimeException ignored) {
        }
        unlocked = null;
    }

    @Override
    public void onListenerDisconnected() {
        stopWaiting();
        if (nowPlaying != null) nowPlaying.close();
        nowPlaying = null;
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        if (nowPlaying != null) nowPlaying.close();
        nowPlaying = null;
        stopWaiting();
        super.onDestroy();
    }
}
