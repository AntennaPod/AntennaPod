package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.widget.ImageButton;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DownloadRequester;

/**
 * Utility methods for the action button that is displayed on the right hand side
 * of a listitem.
 */
class ActionButtonUtils {

    private final int[] labels;
    private final TypedArray drawables;
    private final Context context;

    public ActionButtonUtils(Context context) {
        Validate.notNull(context);

        this.context = context.getApplicationContext();
        drawables = context.obtainStyledAttributes(new int[] {
                R.attr.av_play,
                R.attr.navigation_cancel,
                R.attr.av_download,
                R.attr.av_pause,
                R.attr.navigation_accept,
                R.attr.content_new
        });
        labels = new int[] {
                R.string.play_label,
                R.string.cancel_download_label,
                R.string.download_label,
                R.string.mark_read_label,
                R.string.add_to_queue_label
        };
    }

    /**
     * Sets the displayed bitmap and content description of the given
     * action button so that it matches the state of the FeedItem.
     */
    @SuppressWarnings("ResourceType")
    public void configureActionButton(ImageButton butSecondary, FeedItem item, boolean isInQueue) {
        Validate.isTrue(butSecondary != null && item != null, "butSecondary or item was null");

        final FeedMedia media = item.getMedia();
        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);
            if (!media.isDownloaded()) {
                if (isDownloadingMedia) {
                    // item is being downloaded
                    butSecondary.setVisibility(View.VISIBLE);
                    butSecondary.setImageDrawable(drawables.getDrawable(1));
                    butSecondary.setContentDescription(context.getString(labels[1]));
                } else {
                    // item is not downloaded and not being downloaded
                    if(DefaultActionButtonCallback.userAllowedMobileDownloads() ||
                            !DefaultActionButtonCallback.userChoseAddToQueue() || isInQueue) {
                        butSecondary.setVisibility(View.VISIBLE);
                        butSecondary.setImageDrawable(drawables.getDrawable(2));
                        butSecondary.setContentDescription(context.getString(labels[2]));
                    } else {
                        // mobile download not allowed yet, item is not in queue and user chose add to queue
                        butSecondary.setVisibility(View.VISIBLE);
                        butSecondary.setImageDrawable(drawables.getDrawable(5));
                        butSecondary.setContentDescription(context.getString(labels[4]));
                    }
                }
            } else {
                // item is downloaded
                butSecondary.setVisibility(View.VISIBLE);
                if (media.isCurrentlyPlaying()) {
                    butSecondary.setImageDrawable(drawables.getDrawable(3));
                } else {
                    butSecondary.setImageDrawable(drawables.getDrawable(0));
                }
                butSecondary.setContentDescription(context.getString(labels[0]));
            }
        } else {
            if (item.isPlayed()) {
                butSecondary.setVisibility(View.INVISIBLE);
            } else {
                butSecondary.setVisibility(View.VISIBLE);
                butSecondary.setImageDrawable(drawables.getDrawable(4));
                butSecondary.setContentDescription(context.getString(labels[3]));
            }
        }
    }
}
