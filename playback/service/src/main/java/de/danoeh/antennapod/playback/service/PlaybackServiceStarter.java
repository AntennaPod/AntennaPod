package de.danoeh.antennapod.playback.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.BuildConfig;

import java.util.concurrent.ExecutionException;

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
        Intent launchIntent = new Intent(context,
                BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE ? Media3PlaybackService.class : PlaybackService.class);
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_PLAYABLE, (Parcelable) media);
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME, shouldStreamThisTime);
        return launchIntent;
    }

    public void start() {
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            SessionToken sessionToken = new SessionToken(context,
                    new ComponentName(context, Media3PlaybackService.class));
            ListenableFuture<MediaController> controllerFuture =
                    new MediaController.Builder(context, sessionToken).buildAsync();
            controllerFuture.addListener(() -> {
                try {
                    MediaController controller = controllerFuture.get();
                    controller.setMediaItem(MediaItemAdapter.fromPlayable(media));
                    controller.prepare();
                    controller.play();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, MoreExecutors.directExecutor());
        }

        if (PlaybackService.isRunning && !callEvenIfRunning) {
            return;
        }
        ContextCompat.startForegroundService(context, getIntent());
    }
}
