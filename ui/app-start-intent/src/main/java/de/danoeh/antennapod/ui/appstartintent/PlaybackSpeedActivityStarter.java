package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Launches the playback speed dialog activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class PlaybackSpeedActivityStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.PLAYBACK_SPEED";
    private final Intent intent;
    private final Context context;

    public PlaybackSpeedActivityStarter(Context context) {
        this.context = context;
        intent = new Intent(INTENT);
        intent.setPackage(context.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
    }

    public Intent getIntent() {
        return intent;
    }

    public PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(context, R.id.pending_intent_playback_speed, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    public void start() {
        context.startActivity(getIntent());
    }
}
