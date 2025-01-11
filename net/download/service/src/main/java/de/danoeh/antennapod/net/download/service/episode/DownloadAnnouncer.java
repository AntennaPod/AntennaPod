package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import de.danoeh.antennapod.net.download.service.R;

public abstract class DownloadAnnouncer {
    public static void announceStart(Context context, String episodeTitle) {
        announce(context, R.string.download_started_talkback, episodeTitle);
    }

    public static void announceCompleted(Context context, String episodeTitle) {
        announce(context, R.string.download_completed_talkback, episodeTitle);
    }

    private static void announce(Context context, int messageTemplate, String episodeTitle) {
        String message = context.getString(messageTemplate, episodeTitle);
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(message);
            am.sendAccessibilityEvent(event);
        }
    }
}
