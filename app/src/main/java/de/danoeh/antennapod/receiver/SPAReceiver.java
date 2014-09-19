package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;

/**
 * Receives intents from AntennaPod Single Purpose apps
 */
public class SPAReceiver extends BroadcastReceiver{
    private static final String TAG = "SPAReceiver";

    public static final String ACTION_SP_APPS_QUERY_FEEDS = "de.danoeh.antennapdsp.intent.SP_APPS_QUERY_FEEDS";
    public static final String ACTION_SP_APPS_QUERY_FEEDS_REPSONSE = "de.danoeh.antennapdsp.intent.SP_APPS_QUERY_FEEDS_RESPONSE";
    public static final String ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA = "feeds";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (StringUtils.equals(intent.getAction(), ACTION_SP_APPS_QUERY_FEEDS_REPSONSE)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Received SP_APPS_QUERY_RESPONSE");
            if (intent.hasExtra(ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA)) {
                String[] feedUrls = intent.getStringArrayExtra(ACTION_SP_APPS_QUERY_FEEDS_REPSONSE_FEEDS_EXTRA);
                if (feedUrls != null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Received feeds list: " + Arrays.toString(feedUrls));
                    for (String url : feedUrls) {
                        Feed f  = new Feed(url, new Date());
                        try {
                            DownloadRequester.getInstance().downloadFeed(context, f);
                        } catch (DownloadRequestException e) {
                            Log.e(TAG, "Error while trying to add feed " + url);
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(context, R.string.sp_apps_importing_feeds_msg, Toast.LENGTH_LONG).show();

                } else {
                    Log.e(TAG, "Received invalid SP_APPS_QUERY_REPSONSE: extra was null");
                }
            } else {
                Log.e(TAG, "Received invalid SP_APPS_QUERY_RESPONSE: Contains no extra");
            }
        }
    }
}
