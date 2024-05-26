package de.danoeh.antennapod.playback.service.internal;

import android.app.Notification;
import android.util.Log;

import androidx.core.app.ServiceCompat;
import de.danoeh.antennapod.playback.service.PlaybackService;

public class PlaybackServiceStateManager {
    private static final String TAG = "PlaybackSrvState";
    private final PlaybackService playbackService;

    private volatile boolean isInForeground = false;
    private volatile boolean hasReceivedValidStartCommand = false;

    public PlaybackServiceStateManager(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    public void startForeground(int notificationId, Notification notification) {
        Log.d(TAG, "startForeground");
        playbackService.startForeground(notificationId, notification);
        isInForeground = true;
    }

    public void stopService() {
        Log.d(TAG, "stopService");
        stopForeground(true);
        playbackService.stopSelf();
        hasReceivedValidStartCommand = false;
    }

    public void stopForeground(boolean removeNotification) {
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

    public boolean hasReceivedValidStartCommand() {
        return hasReceivedValidStartCommand;
    }

    public void validStartCommandWasReceived() {
        this.hasReceivedValidStartCommand = true;
    }
}
