package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.core.storage.DBTasks;

public class FeedUpdateUtils {
    private static final String TAG = "FeedUpdateUtils";

    private FeedUpdateUtils() {

    }

    public static void startAutoUpdate(Context context, boolean synchronously) {
        if (NetworkUtils.networkAvailable() && NetworkUtils.isDownloadAllowed()) {
            if (synchronously) {
                DBTasks.refreshAllFeedsSynchronously(context, null);
            } else {
                DBTasks.refreshAllFeeds(context, null);
            }
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }
    }
}
