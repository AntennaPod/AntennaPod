package de.danoeh.antennapod.ui.appstartintent;

import android.content.Context;
import android.content.Intent;

/**
 * Launches the video player activity of the app with specific arguments.
 * Does not require a dependency on the actual implementation of the activity.
 */
public class VideoPlayerActivityStarter {
    public static final String INTENT_MEDIA3 = "de.danoeh.antennapod.intents.VIDEO_PLAYER_MEDIA3";
    private final Intent intent;

    public VideoPlayerActivityStarter(Context context) {
        intent = new Intent(INTENT_MEDIA3);
        intent.setPackage(context.getPackageName());
    }

    public Intent getIntent() {
        return intent;
    }
}
