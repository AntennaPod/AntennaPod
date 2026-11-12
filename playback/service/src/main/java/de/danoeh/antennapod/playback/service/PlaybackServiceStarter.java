package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.BuildConfig;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;

import java.util.Objects;

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
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_PLAYABLE, (Parcelable) media);
        launchIntent.putExtra(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME, shouldStreamThisTime);
        return launchIntent;
    }

    public void start() {
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            PlaybackController.bindToMedia3Service(context, controller -> {
                FeedMedia feedMedia = (FeedMedia) media;
                if (isCurrentMediaItem(feedMedia, controller.getCurrentMediaItem())) {
                    controller.play();
                    return;
                }
                if (!controller.isPlaying() && controller.getDeviceInfo().playbackType
                        == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    controller.play(); // Casting somehow does not play when not quickly starting the old episode
                }
                controller.setMediaItem(MediaItemAdapter.fromMediaIdStub(feedMedia.getId()));
                controller.prepare();
                controller.play();
            });
            return;
        }

        if (PlaybackServiceInterface.isPlaying && !callEvenIfRunning) {
            return;
        }
        ContextCompat.startForegroundService(context, getIntent());
    }

    static boolean isCurrentMediaItem(FeedMedia media, MediaItem currentMediaItem) {
        if (currentMediaItem == null || currentMediaItem.localConfiguration == null
                || !("" + media.getId()).equals(currentMediaItem.mediaId)) {
            return false;
        }
        String mediaUrl = media.localFileAvailable() ? media.getLocalFileUrl() : media.getStreamUrl();
        return Objects.equals(mediaUrl, currentMediaItem.localConfiguration.uri.toString());
    }
}
