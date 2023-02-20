package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;

/**
 * Launches the download authentication activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class DownloadAuthenticationActivityStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.DOWNLOAD_AUTH_ACTIVITY";
    public static final String EXTRA_DOWNLOAD_REQUEST = "download_request";

    private final Intent intent;
    private final Context context;
    private final long feedFileId;

    public DownloadAuthenticationActivityStarter(Context context, long feedFileId, Parcelable downloadRequest) {
        this.context = context;
        this.feedFileId = feedFileId;
        intent = new Intent(INTENT);
        intent.setAction("request" + feedFileId);
        intent.putExtra(EXTRA_DOWNLOAD_REQUEST, downloadRequest);
        intent.setPackage(context.getPackageName());
    }

    public Intent getIntent() {
        return intent;
    }

    public PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(context.getApplicationContext(),
                ("downloadAuth" + feedFileId).hashCode(), getIntent(),
                PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
}
