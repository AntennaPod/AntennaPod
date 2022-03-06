package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;

import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.model.playback.Playable;

public class PlaybackServiceStarter {
    private final Context context;
    private final Playable media;
    private boolean shouldStreamThisTime = false;
    private boolean callEvenIfRunning = false;

    public PlaybackServiceStarter(Context context, Playable media) {
        this.context = context;
        this.media = media;
    }

    /**
     * Default value: false
     */
    public PlaybackServiceStarter callEvenIfRunning(boolean callEvenIfRunning) {
        this.callEvenIfRunning = callEvenIfRunning;
        return this;
    }

    public PlaybackServiceStarter shouldStreamThisTime(boolean shouldStreamThisTime) {
        this.shouldStreamThisTime = shouldStreamThisTime;
        return this;
    }

    public Intent getIntent() {
        Intent launchIntent = new Intent(context, PlaybackService.class);
        launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, (Parcelable) media);
        launchIntent.putExtra(PlaybackService.EXTRA_ALLOW_STREAM_THIS_TIME, shouldStreamThisTime);
        return launchIntent;
    }

    public void start() {
        if (PlaybackService.isRunning && !callEvenIfRunning) {
            return;
        }
        ContextCompat.startForegroundService(context, getIntent());
    }
}
