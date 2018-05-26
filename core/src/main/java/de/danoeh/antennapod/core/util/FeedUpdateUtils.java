package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.core.storage.DBTasks;

public class FeedUpdateUtils {
    private static final String TAG = "FeedUpdateUtils";

    private FeedUpdateUtils() {

    }

    public static void startAutoUpdate(Context context, Runnable callback) {
        if (NetworkUtils.networkAvailable() && NetworkUtils.isDownloadAllowed()) {
            DBTasks.refreshAllFeeds(context, null, callback);
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }
    }
}
