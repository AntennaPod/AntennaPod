package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;

import static de.danoeh.antennapod.core.service.playback.PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE;
import static de.danoeh.antennapod.core.service.playback.PlaybackService.ACTION_RESUME_PLAY_CURRENT_EPISODE;

public class StreamActionButton extends ItemActionButton {

    StreamActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.stream_label;
    }

    @Override
    @AttrRes
    public int getDrawable() {
        FeedMedia media = item.getMedia();
        if (media != null && media.isCurrentlyPlaying()) {
            return R.attr.av_pause;
        }
        return R.attr.action_stream;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        if (media.isPlaying()) {
            togglePlayPause(context, media);
        } else {
            DBTasks.playMedia(context, media, false, true, true);
        }
    }

    private void togglePlayPause(Context context, FeedMedia media) {
        new PlaybackServiceStarter(context, media)
                .startWhenPrepared(true)
                .shouldStream(true)
                .start();

        String pauseOrResume = media.isCurrentlyPlaying()
                ? ACTION_PAUSE_PLAY_CURRENT_EPISODE : ACTION_RESUME_PLAY_CURRENT_EPISODE;
        IntentUtils.sendLocalBroadcast(context, pauseOrResume);
    }
}
