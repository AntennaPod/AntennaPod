package de.danoeh.antennapod.config;

import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.support.annotation.NonNull;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.CastplayerActivity;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import de.danoeh.antennapod.core.PlaybackServiceCallbacks;
import de.danoeh.antennapod.core.feed.MediaType;


public class PlaybackServiceCallbacksImpl implements PlaybackServiceCallbacks {
    @NonNull
    @Override
    public Intent getPlayerActivityIntent(Context context, MediaType mediaType, boolean remotePlayback) {
        if (remotePlayback) {
            return new Intent(context, CastplayerActivity.class);
        }
        if (mediaType == MediaType.VIDEO) {
            Intent i = new Intent(context, VideoplayerActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            }
            return i;
        } else {
            return new Intent(context, AudioplayerActivity.class);
        }
    }

    @Override
    public boolean useQueue() {
        return true;
    }

    @Override
    public int getNotificationIconResource(Context context) {
        return R.drawable.ic_stat_antenna_default;
    }
}
