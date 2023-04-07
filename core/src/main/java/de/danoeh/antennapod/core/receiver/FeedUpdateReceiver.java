package de.danoeh.antennapod.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;

/**
 * Refreshes all feeds when it receives an intent
 */
public class FeedUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "FeedUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent");
        ClientConfigurator.initialize(context);

        FeedUpdateManager.runOnce(context);
    }

}
