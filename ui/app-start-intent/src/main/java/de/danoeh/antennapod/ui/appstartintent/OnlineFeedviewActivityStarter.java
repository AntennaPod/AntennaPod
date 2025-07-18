package de.danoeh.antennapod.ui.appstartintent;

import android.content.Context;
import android.content.Intent;

public class OnlineFeedviewActivityStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.ONLINE_FEEDVIEW";
    public static final String ARG_FEEDURL = "arg.feedurl";
    public static final String ARG_WAS_MANUAL_URL = "manual_url";
    private final Intent intent;

    public OnlineFeedviewActivityStarter(Context context, String feedUrl) {
        intent = new Intent(INTENT);
        intent.setPackage(context.getPackageName());
        intent.putExtra(ARG_FEEDURL, feedUrl);
    }

    public OnlineFeedviewActivityStarter withManualUrl() {
        intent.putExtra(ARG_WAS_MANUAL_URL, true);
        return this;
    }

    public Intent getIntent() {
        return intent;
    }
}
