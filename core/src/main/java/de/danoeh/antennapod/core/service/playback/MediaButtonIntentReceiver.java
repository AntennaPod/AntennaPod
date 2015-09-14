package de.danoeh.antennapod.core.service.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final String TAG = "MediaButtonIntentRcver";

    private static PlaybackServiceMediaPlayer mMediaPlayer;

    public static void setMediaPlayer(PlaybackServiceMediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(Context, " + intent.toString() +")");
        if (mMediaPlayer != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            mMediaPlayer.handleMediaKey(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
        }
    }

}