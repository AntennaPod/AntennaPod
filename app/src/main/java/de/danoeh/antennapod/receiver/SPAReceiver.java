package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.model.feed.Feed;

/**
 * Receives intents from AntennaPod Single Purpose apps
 */
public class SPAReceiver extends BroadcastReceiver{
    private static final String TAG = "SPAReceiver";

    public static final String ACTION_SP_APPS_QUERY_FEEDS = "de.danoeh.antennapdsp.intent.SP_APPS_QUERY_FEEDS";
    private static final String ACTION_SP_APPS_QUERY_FEEDS_REPSONSE = "de.danoeh.antennapdsp.intent.SP_APPS_QUERY_FEEDS_RESPONSE";
    private static final String ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA = "feeds";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(intent.getAction(), ACTION_SP_APPS_QUERY_FEEDS_REPSONSE)) {
            return;
        }
        Log.d(TAG, "Received SP_APPS_QUERY_RESPONSE");
        if (!intent.hasExtra(ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA)) {
            Log.e(TAG, "Received invalid SP_APPS_QUERY_RESPONSE: Contains no extra");
            return;
        }
        String[] feedUrls = intent.getStringArrayExtra(ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA);
        if (feedUrls == null) {
            Log.e(TAG, "Received invalid SP_APPS_QUERY_REPSONSE: extra was null");
            return;
        }
        Log.d(TAG, "Received feeds list: " + Arrays.toString(feedUrls));
        ClientConfigurator.initialize(context);
        for (String url : feedUrls) {
            Feed feed = new Feed(url, null, "Unknown podcast");
            feed.setItems(Collections.emptyList());
            DBTasks.updateFeed(context, feed, false);
        }
        Toast.makeText(context, R.string.sp_apps_importing_feeds_msg, Toast.LENGTH_LONG).show();
        FeedUpdateManager.runOnce(context);
    }
}
