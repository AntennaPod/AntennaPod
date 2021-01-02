package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

public class PlaybackServiceStarter {
    private final Context context;
    private final Playable media;
    private boolean startWhenPrepared = false;
    private boolean shouldStream = false;
    private boolean shouldStreamThisTime = false;
    private boolean callEvenIfRunning = false;
    private boolean prepareImmediately = true;

    public PlaybackServiceStarter(Context context, Playable media) {
        this.context = context;
        this.media = media;
    }

    /**
     * Default value: false
     */
    public PlaybackServiceStarter shouldStream(boolean shouldStream) {
        this.shouldStream = shouldStream;
        return this;
    }

    public PlaybackServiceStarter streamIfLastWasStream() {
        boolean lastIsStream = PlaybackPreferences.getCurrentEpisodeIsStream();
        return shouldStream(lastIsStream);
    }

    /**
     * Default value: false
     */
    public PlaybackServiceStarter startWhenPrepared(boolean startWhenPrepared) {
        this.startWhenPrepared = startWhenPrepared;
        return this;
    }

    /**
     * Default value: false
     */
    public PlaybackServiceStarter callEvenIfRunning(boolean callEvenIfRunning) {
        this.callEvenIfRunning = callEvenIfRunning;
        return this;
    }

    /**
     * Default value: true
     */
    public PlaybackServiceStarter prepareImmediately(boolean prepareImmediately) {
        this.prepareImmediately = prepareImmediately;
        return this;
    }

    public PlaybackServiceStarter shouldStreamThisTime(boolean shouldStreamThisTime) {
        this.shouldStreamThisTime = shouldStreamThisTime;
        return this;
    }

    public Intent getIntent() {
        Intent launchIntent = new Intent(context, PlaybackService.class);
        launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
        launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED, startWhenPrepared);
        launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, shouldStream);
        launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY, prepareImmediately);
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
