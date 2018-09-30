package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.util.Log;

import de.danoeh.antennapod.core.storage.DBTasks;

public class FeedUpdateUtils {
    private static final String TAG = "FeedUpdateUtils";

    private FeedUpdateUtils() {

    }

    public static void startAutoUpdate(Context context, Runnable callback) {
        if (NetworkUtils.networkAvailable()) {
            refreshAllFeedsIfDownloadAllowed(context, callback);
        } else if (NetworkUtils.networkProbablyConnected()){
            Log.d(TAG, "Workaround for #2691: Android probably incorrectly reports network disconnected. Treat the network is connected and proceed to refresh feeds.");
            refreshAllFeedsIfDownloadAllowed(context, callback);
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }
    }

    private static void refreshAllFeedsIfDownloadAllowed(Context context, Runnable callback) {
        if (NetworkUtils.isDownloadAllowed()) {
            DBTasks.refreshAllFeeds(context, null, callback);
        }
    }

}
