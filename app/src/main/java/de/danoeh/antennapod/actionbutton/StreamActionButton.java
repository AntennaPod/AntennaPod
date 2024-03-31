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
import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.ui.StreamingConfirmationDialog;

public class StreamActionButton extends ItemActionButton {

    public StreamActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.stream_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_stream;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }
        UsageStatistics.logAction(UsageStatistics.ACTION_STREAM);

        if (!NetworkUtils.isStreamingAllowed()) {
            new StreamingConfirmationDialog(context, media).show();
            return;
        }
        new PlaybackServiceStarter(context, media)
                .callEvenIfRunning(true)
                .start();

        if (media.getMediaType() == MediaType.VIDEO) {
            context.startActivity(PlaybackService.getPlayerActivityIntent(context, media));
        }
    }
}
