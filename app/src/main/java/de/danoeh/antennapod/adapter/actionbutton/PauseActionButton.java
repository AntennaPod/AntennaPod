package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.IntentUtils;

import static de.danoeh.antennapod.core.service.playback.PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE;

public class PauseActionButton extends ItemActionButton {

    public PauseActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.pause_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_pause;
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        if (FeedItemUtil.isCurrentlyPlaying(media)) {
            IntentUtils.sendLocalBroadcast(context, ACTION_PAUSE_PLAY_CURRENT_EPISODE);
        }
    }
}
