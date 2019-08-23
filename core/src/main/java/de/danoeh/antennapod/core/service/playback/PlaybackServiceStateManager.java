package de.danoeh.antennapod.core.service.playback;

import android.app.Notification;

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
        isInForeground = false;
        playbackService.stopSelf();
    }

    void stopForeground(boolean removeNotification) {
        playbackService.stopForeground(removeNotification);
        isInForeground = false;
        hasReceivedValidStartCommand = false;
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
