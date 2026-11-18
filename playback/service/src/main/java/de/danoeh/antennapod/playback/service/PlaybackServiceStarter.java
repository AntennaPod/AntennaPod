package de.danoeh.antennapod.playback.service;

import android.content.Context;
import androidx.media3.common.DeviceInfo;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;

public class PlaybackServiceStarter {
    private final Context context;
    private final Playable media;

    public PlaybackServiceStarter(Context context, Playable media) {
        this.context = context;
        this.media = media;
    }

    public PlaybackServiceStarter callEvenIfRunning(boolean callEvenIfRunning) {
        return this;
    }

    public void start() {
        PlaybackController.bindToMedia3Service(context, controller -> {
            if (controller.getCurrentMediaItem() != null && media instanceof FeedMedia
                    && ("" + ((FeedMedia) media).getId()).equals(controller.getCurrentMediaItem().mediaId)) {
                controller.play();
                return;
            }
            if (!controller.isPlaying() && controller.getDeviceInfo().playbackType
                    == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                controller.play(); // Casting somehow does not play when not quickly starting the old episode
            }
            controller.setMediaItem(MediaItemAdapter.fromMediaIdStub(((FeedMedia) media).getId()));
            controller.prepare();
            controller.play();
        });
    }
}
