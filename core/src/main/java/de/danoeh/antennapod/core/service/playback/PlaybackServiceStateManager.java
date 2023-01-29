package de.danoeh.antennapod.core.service.playback;

import android.app.Notification;
import android.util.Log;

import androidx.core.app.ServiceCompat;

class PlaybackServiceStateManager {
    private static final String TAG = "PlaybackSrvState";
    private final PlaybackService playbackService;

    private volatile boolean isInForeground = false;
    private volatile boolean hasReceivedValidStartCommand = false;

    PlaybackServiceStateManager(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    void startForeground(int notificationId, Notification notification) {
        Log.d(TAG, "startForeground");
        playbackService.startForeground(notificationId, notification);
        isInForeground = true;
    }

    void stopService() {
        Log.d(TAG, "stopService");
        stopForeground(true);
        playbackService.stopSelf();
        hasReceivedValidStartCommand = false;
    }

    void stopForeground(boolean removeNotification) {
        Log.d(TAG, "stopForeground");
        if (isInForeground) {
            if (removeNotification) {
                ServiceCompat.stopForeground(playbackService, ServiceCompat.STOP_FOREGROUND_REMOVE);
            } else {
                ServiceCompat.stopForeground(playbackService, ServiceCompat.STOP_FOREGROUND_DETACH);
            }
        }
        isInForeground = false;
    }

    boolean hasReceivedValidStartCommand() {
        return hasReceivedValidStartCommand;
    }

    void validStartCommandWasReceived() {
        this.hasReceivedValidStartCommand = true;
    }
}
