package de.danoeh.antennapod.adapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.util.Converter;

/**
 * Utility methods for adapters
 */
public class AdapterUtils {

    private AdapterUtils() {

    }

    /**
     * Updates the contents of the TextView that shows the current playback position and the ProgressBar.
     */
    public static void updateEpisodePlaybackProgress(FeedItem item, Resources res, TextView txtvPos, ProgressBar episodeProgress) {
        FeedMedia media = item.getMedia();
        episodeProgress.setVisibility(View.GONE);
        if (media == null) {
            txtvPos.setVisibility(View.GONE);
            return;
        } else {
            txtvPos.setVisibility(View.VISIBLE);
        }

        FeedItem.State state = item.getState();
        if (state == FeedItem.State.PLAYING
                || state == FeedItem.State.IN_PROGRESS) {
            if (media.getDuration() > 0) {
                episodeProgress.setVisibility(View.VISIBLE);
                episodeProgress
                        .setProgress((int) (((double) media
                                .getPosition()) / media.getDuration() * 100));
                txtvPos.setText(Converter
                        .getDurationStringLong(media.getDuration()
                                - media.getPosition()));
            }
        } else if (!media.isDownloaded()) {
            txtvPos.setText(res.getString(
                    R.string.size_prefix)
                    + Converter.byteToString(media.getSize()));
        } else {
            txtvPos.setText(res.getString(
                    R.string.length_prefix)
                    + Converter.getDurationStringLong(media
                    .getDuration()));
        }
    }
}
