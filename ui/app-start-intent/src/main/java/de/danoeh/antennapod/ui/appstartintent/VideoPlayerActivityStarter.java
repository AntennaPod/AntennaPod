package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Launches the video player activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class VideoPlayerActivityStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.VIDEO_PLAYER";
    public static final String INTENT_MEDIA3 = "de.danoeh.antennapod.intents.VIDEO_PLAYER_MEDIA3";
    private final Intent intent;
    private final Context context;

    public VideoPlayerActivityStarter(Context context) {
        this.context = context;
        intent = new Intent(BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE ? INTENT_MEDIA3 : INTENT);
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    }

    public Intent getIntent() {
        return intent;
    }

    public PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(context, R.id.pending_intent_video_player, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public void start() {
        context.startActivity(getIntent());
    }
}
