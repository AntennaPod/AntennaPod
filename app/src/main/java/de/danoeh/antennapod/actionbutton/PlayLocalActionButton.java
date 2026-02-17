package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;

public class PlayLocalActionButton extends ItemActionButton {

    public PlayLocalActionButton(FeedItem item, boolean queueContext) {
        super(item, queueContext);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.play_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_play_24dp;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        logPlaybackDebug(context, "PlayLocalActionButton onClick queueContext=" + isQueueContext()
                + ", itemId=" + item.getId() + ", mediaId=" + media.getId());

        new PlaybackServiceStarter(context, media)
                .callEvenIfRunning(true)
            .setAutoAdvanceMode(isQueueContext()
                ? PlaybackPreferences.AUTO_ADVANCE_QUEUE
                : PlaybackPreferences.AUTO_ADVANCE_PODCAST)
                .start();

        if (media.getMediaType() == MediaType.VIDEO) {
            context.startActivity(PlaybackService.getPlayerActivityIntent(context, media));
        }
    }
}
