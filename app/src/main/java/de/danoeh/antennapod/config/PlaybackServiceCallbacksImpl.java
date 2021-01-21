package de.danoeh.antennapod.config;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import de.danoeh.antennapod.core.PlaybackServiceCallbacks;
import de.danoeh.antennapod.core.feed.MediaType;

public class PlaybackServiceCallbacksImpl implements PlaybackServiceCallbacks {
    @Override
    public Intent getPlayerActivityIntent(Context context, MediaType mediaType, boolean remotePlayback) {
        if (mediaType == MediaType.AUDIO || remotePlayback) {
            return new Intent(context, MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true);
        } else {
            Intent i = new Intent(context, VideoplayerActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            }
            return i;
        }
    }
}
