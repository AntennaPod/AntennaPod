package de.danoeh.antennapod.core.service.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final String TAG = "MediaButtonIntentRcver";

    private static PlaybackService mPlaybackService;

    public static void setMediaPlayer(PlaybackService playbackService) {
        mPlaybackService = playbackService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(Context, " + intent.toString() +")");
        if (mPlaybackService != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                mPlaybackService.handleKeycode(event.getKeyCode(), event.getSource());
            }
        }
    }

}