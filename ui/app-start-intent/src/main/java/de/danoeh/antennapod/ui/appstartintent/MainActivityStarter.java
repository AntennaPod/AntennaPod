package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * Launches the main activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class MainActivityStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.MAIN_ACTIVITY";
    public static final String EXTRA_OPEN_PLAYER = "open_player";
    public static final String EXTRA_FEED_ID = "fragment_feed_id";
    public static final String EXTRA_ADD_TO_BACK_STACK = "add_to_back_stack";
    public static final String EXTRA_FRAGMENT_TAG = "fragment_tag";
    public static final String EXTRA_OPEN_DRAWER = "open_drawer";
    public static final String EXTRA_OPEN_DOWNLOAD_LOGS = "open_download_logs";
    public static final String EXTRA_FRAGMENT_ARGS = "fragment_args";

    private final Intent intent;
    private final Context context;
    private Bundle fragmentArgs = null;

    public MainActivityStarter(Context context) {
        this.context = context;
        intent = new Intent(INTENT);
        intent.setPackage(context.getPackageName());
    }

    public Intent getIntent() {
        if (fragmentArgs != null) {
            intent.putExtra(EXTRA_FRAGMENT_ARGS, fragmentArgs);
        }
        return intent;
    }

    public PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(context, R.id.pending_intent_player_activity, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    public void start() {
        context.startActivity(getIntent());
    }

    public MainActivityStarter withOpenPlayer() {
        intent.putExtra(EXTRA_OPEN_PLAYER, true);
        return this;
    }

    public MainActivityStarter withOpenFeed(long feedId) {
        intent.putExtra(EXTRA_FEED_ID, feedId);
        return this;
    }

    public MainActivityStarter withAddToBackStack() {
        intent.putExtra(EXTRA_ADD_TO_BACK_STACK, true);
        return this;
    }

    public MainActivityStarter withFragmentLoaded(String fragmentName) {
        intent.putExtra(EXTRA_FRAGMENT_TAG, fragmentName);
        return this;
    }

    public MainActivityStarter withDrawerOpen() {
        intent.putExtra(EXTRA_OPEN_DRAWER, true);
        return this;
    }

    public MainActivityStarter withDownloadLogsOpen() {
        intent.putExtra(EXTRA_OPEN_DOWNLOAD_LOGS, true);
        return this;
    }

    public MainActivityStarter withFragmentArgs(String name, boolean value) {
        if (fragmentArgs == null) {
            fragmentArgs = new Bundle();
        }
        fragmentArgs.putBoolean(name, value);
        return this;
    }
}
