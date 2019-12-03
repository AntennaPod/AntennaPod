package de.danoeh.antennapodSA.config;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.PlaybackServiceCallbacks;
import de.danoeh.antennapodSA.core.feed.MediaType;
import de.danoeh.antennapodSA.activity.AudioplayerActivity;
import de.danoeh.antennapodSA.activity.CastplayerActivity;
import de.danoeh.antennapodSA.activity.VideoplayerActivity;


public class PlaybackServiceCallbacksImpl implements PlaybackServiceCallbacks {
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
        return R.drawable.ic_antenna;
    }
}
