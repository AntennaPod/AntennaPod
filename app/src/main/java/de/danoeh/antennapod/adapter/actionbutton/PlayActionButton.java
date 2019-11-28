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

class PlayActionButton extends ItemActionButton {

    PlayActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.play_label;
    }

    @Override
    @AttrRes
    public int getDrawable() {
        FeedMedia media = item.getMedia();
        if (media != null && media.isCurrentlyPlaying()) {
            return R.attr.av_pause;
        } else {
            return R.attr.av_play;
        }
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        if (media.isPlaying()) {
            togglePlayPause(context, media);
        } else {
            DBTasks.playMedia(context, media, false, true, false);
        }
    }

    private void togglePlayPause(Context context, FeedMedia media) {
        if (media.isCurrentlyPlaying()) {
            IntentUtils.sendLocalBroadcast(context, ACTION_PAUSE_PLAY_CURRENT_EPISODE);
        } else {
            new PlaybackServiceStarter(context, media)
                    .callEvenIfRunning(true)
                    .startWhenPrepared(true)
                    .shouldStream(false)
                    .start();
        }
    }
}
