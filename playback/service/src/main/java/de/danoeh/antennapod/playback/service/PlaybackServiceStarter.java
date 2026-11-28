package de.danoeh.antennapod.playback.service;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;

public class PlaybackServiceStarter {
    private final Context context;
    private final FeedMedia media;
    private long startPositionMs = C.TIME_UNSET;

    public PlaybackServiceStarter(Context context, FeedMedia media) {
        this.context = context;
        this.media = media;
    }

    public PlaybackServiceStarter withStartPosition(long startPositionMs) {
        this.startPositionMs = startPositionMs;
        return this;
    }

    public void start() {
        PlaybackController.bindToMedia3Service(context, controller -> {
            if (controller.getCurrentMediaItem() != null
                    && ("" + media.getId()).equals(controller.getCurrentMediaItem().mediaId)) {
                controller.play();
                if (startPositionMs != C.TIME_UNSET) {
                    controller.seekTo(startPositionMs);
                }
                return;
            }
            if (!controller.isPlaying() && controller.getDeviceInfo().playbackType
                    == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                controller.play(); // Casting somehow does not play when not quickly starting the old episode
            }
            controller.setMediaItem(MediaItemAdapter.fromMediaIdStub(media.getId()), startPositionMs);
            controller.prepare();
            controller.play();
        });
    }
}
