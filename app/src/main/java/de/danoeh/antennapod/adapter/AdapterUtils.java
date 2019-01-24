package de.danoeh.antennapod.adapter;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joanzapata.iconify.Iconify;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * Utility methods for adapters
 */
class AdapterUtils {

    private static final String TAG = AdapterUtils.class.getSimpleName();

    private AdapterUtils() {

    }

    /**
     * Updates the contents of the TextView that shows the current playback position and the ProgressBar.
     */
    static void updateEpisodePlaybackProgress(FeedItem item, TextView txtvPos, ProgressBar episodeProgress) {
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
                episodeProgress.setProgress((int) (((double) media
                                .getPosition()) / media.getDuration() * 100));
                txtvPos.setText(Converter.getDurationStringLong(media.getDuration()
                                - media.getPosition()));
            }
        } else {
            txtvPos.setText(Converter.getDurationStringLong(media.getDuration()));
        }
    }
}
