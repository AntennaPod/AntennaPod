package de.danoeh.antennapod.core.service.playback;

import android.app.Notification;
import android.app.Service;
import android.os.Build;

class PlaybackServiceStateManager {
    private final PlaybackService playbackService;

    private volatile boolean isInForeground = false;
    private volatile boolean hasReceivedValidStartCommand = false;

    PlaybackServiceStateManager(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    void startForeground(int notificationId, Notification notification) {
        playbackService.startForeground(notificationId, notification);
        isInForeground = true;
    }

    void stopService() {
        stopForeground(true);
        playbackService.stopSelf();
        hasReceivedValidStartCommand = false;
    }

    void stopForeground(boolean removeNotification) {
        if (isInForeground) {
            if (Build.VERSION.SDK_INT < 24) {
                playbackService.stopForeground(removeNotification);
            } else if (removeNotification) {
                playbackService.stopForeground(Service.STOP_FOREGROUND_REMOVE);
            } else {
                playbackService.stopForeground(Service.STOP_FOREGROUND_DETACH);
            }
        }
        isInForeground = false;
    }

    boolean isInForeground() {
        return isInForeground;
    }

    boolean hasReceivedValidStartCommand() {
        return hasReceivedValidStartCommand;
    }

    void validStartCommandWasReceived() {
        this.hasReceivedValidStartCommand = true;
    }
}
