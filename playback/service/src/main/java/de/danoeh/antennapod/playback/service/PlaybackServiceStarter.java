package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.BuildConfig;
import de.danoeh.antennapod.playback.service.internal.MediaItemAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PlaybackServiceStarter {
    private final Context context;
    private final Playable media;
    private boolean shouldStreamThisTime = false;
    private boolean callEvenIfRunning = false;
    private Integer autoAdvanceMode = null;
    private static final String TAG = "PlaybackServiceStarter";
    private static final String DEBUG_LOG_FILE = "autoplay_debug.log";

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

    public PlaybackServiceStarter setAutoAdvanceMode(int autoAdvanceMode) {
        this.autoAdvanceMode = autoAdvanceMode;
        return this;
    }

    public Intent getIntent() {
        Intent launchIntent = new Intent(context, PlaybackService.class);
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_PLAYABLE, (Parcelable) media);
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME, shouldStreamThisTime);
        if (autoAdvanceMode != null) {
            launchIntent.putExtra(PlaybackServiceInterface.EXTRA_AUTO_ADVANCE_MODE, autoAdvanceMode);
        }
        return launchIntent;
    }

    public void start() {
        if (autoAdvanceMode != null) {
            PlaybackPreferences.setAutoAdvanceMode(autoAdvanceMode);
        }
        Log.d(TAG, "start: autoAdvanceMode=" + autoAdvanceMode + ", playable=" + media);
        logDebug("PlaybackServiceStarter.start playable="
                + (media != null ? media.getEpisodeTitle() : "null")
                + ", class=" + (media != null ? media.getClass().getSimpleName() : "null")
                + ", autoAdvanceMode=" + autoAdvanceMode);
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            PlaybackController.bindToMedia3Service(context, controller -> {
                controller.setMediaItem(MediaItemAdapter.fromPlayable(media));
                controller.prepare();
                controller.play();
            });
            return;
        }

        if (PlaybackService.isRunning && !callEvenIfRunning) {
            return;
        }
        ContextCompat.startForegroundService(context, getIntent());
    }

    private void logDebug(String message) {
        Log.d(TAG, message);
        if (context == null) {
            return;
        }
        try {
            File logFile = new File(context.getExternalFilesDir(null), DEBUG_LOG_FILE);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(System.currentTimeMillis() + ": " + message + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "logDebug write failed", e);
        }
    }
}
