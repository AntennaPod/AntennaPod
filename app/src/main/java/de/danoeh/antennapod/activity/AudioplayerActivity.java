package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerInfoActivity {
    public static final String TAG = "AudioPlayerActivity";

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            Log.d(TAG, "Received VIEW intent: " + intent.getData().getPath());
            ExternalMedia media = new ExternalMedia(intent.getData().getPath(),
                    MediaType.AUDIO);
            Intent launchIntent = new Intent(this, PlaybackService.class);
            launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
            launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
                    true);
            launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, false);
            launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
                    true);
            startService(launchIntent);
        }
    }
}
