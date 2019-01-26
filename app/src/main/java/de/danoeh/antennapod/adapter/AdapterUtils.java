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
    static void updateEpisodePlaybackProgress(FeedItem item, TextView txtvPosLeft, TextView txtvPosRight, ProgressBar episodeProgress) {
        FeedMedia media = item.getMedia();
        episodeProgress.setVisibility(View.GONE);
        if (media == null) {
            txtvPosLeft.setVisibility(View.GONE);
            txtvPosRight.setVisibility(View.GONE);
            return;
        } else {
            txtvPosLeft.setVisibility(View.VISIBLE);
            txtvPosRight.setVisibility(View.VISIBLE);
        }

        FeedItem.State state = item.getState();

        if (state == FeedItem.State.PLAYING
                || state == FeedItem.State.IN_PROGRESS) {
            if (media.getDuration() > 0) {
                int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                episodeProgress.setProgress(progress);
                episodeProgress.setVisibility(View.VISIBLE);
                txtvPosLeft.setText(Converter
                        .getDurationStringLong(media.getPosition()));
                txtvPosRight.setText(Converter.getDurationStringLong(media.getDuration()));
            }
        } else {
            if(media.getSize() > 0) {
                txtvPosLeft.setText(Converter.byteToString(media.getSize()));
            } else if(NetworkUtils.isDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
                txtvPosLeft.setText("{fa-spinner}");
                Iconify.addIcons(txtvPosLeft);
                NetworkUtils.getFeedMediaSizeObservable(media)
                        .subscribe(
                                size -> {
                                    if (size > 0) {
                                        txtvPosLeft.setText(Converter.byteToString(size));
                                    } else {
                                        txtvPosLeft.setText("");
                                    }
                                }, error -> {
                                    txtvPosLeft.setText("");
                                    Log.e(TAG, Log.getStackTraceString(error));
                                });
            } else {
                txtvPosLeft.setText("");
            }
            txtvPosRight.setText(Converter.getDurationStringLong(media.getDuration()));
            episodeProgress.setVisibility(View.INVISIBLE);
        }
    }
}
