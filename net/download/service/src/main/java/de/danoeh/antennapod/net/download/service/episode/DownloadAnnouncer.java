package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import de.danoeh.antennapod.net.download.service.R;

public class DownloadAnnouncer {
    private final Context context;

    public DownloadAnnouncer(Context context) {
        this.context = context;
    }

    public void announceDownloadStart(String episodeTitle) {
        String message = context.getString(R.string.download_started_talkback, episodeTitle);
        announceDownloadStatus(message);
    }

    public void announceDownloadEnd(String episodeTitle, boolean success) {
        String message = context.getString(success ? R.string.download_completed_talkback : R.string.download_failed_talkback, episodeTitle);
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