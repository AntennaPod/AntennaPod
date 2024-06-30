package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class DownloadAnnouncer {
    private final Context context;

    public DownloadAnnouncer(Context context) {
        this.context = context;
    }

    public void announceDownloadStart(String episodeTitle) {
        String message = episodeTitle + " download started.";
        announceDownloadStatus(message);
    }

    public void announceDownloadEnd(String episodeTitle, boolean success) {
        String message = episodeTitle + (success ? " download completed." : " download failed.");
        announceDownloadStatus(message);
    }

    private void announceDownloadStatus(String message) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(message);
            am.sendAccessibilityEvent(event);
        }
    }
}