package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LongList;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;

public class FeedItemFilter {

    private final String[] mProperties;

    private boolean showPlayed = false;
    private boolean showUnplayed = false;
    private boolean showPaused = false;
    private boolean showNotPaused = false;
    private boolean showQueued = false;
    private boolean showNotQueued = false;
    private boolean showDownloaded = false;
    private boolean showNotDownloaded = false;
    private boolean showHasMedia = false;
    private boolean showNoMedia = false;
    private boolean showIsFavorite = false;
    private boolean showNotFavorite = false;

    public FeedItemFilter(String properties) {
        this(TextUtils.split(properties, ","));
    }

    public FeedItemFilter(String[] properties) {
        this.mProperties = properties;
        for (String property : properties) {
            // see R.arrays.feed_filter_values
            switch (property) {
                case "unplayed":
                    showUnplayed = true;
                    break;
                case "paused":
                    showPaused = true;
                    break;
                case "not_paused":
                    showNotPaused = true;
                    break;
                case "played":
                    showPlayed = true;
                    break;
                case "queued":
                    showQueued = true;
                    break;
                case "not_queued":
                    showNotQueued = true;
                    break;
                case "downloaded":
                    showDownloaded = true;
                    break;
                case "not_downloaded":
                    showNotDownloaded = true;
                    break;
                case "has_media":
                    showHasMedia = true;
                    break;
                case "no_media":
                    showNoMedia = true;
                    break;
                case "is_favorite":
                    showIsFavorite = true;
                    break;
                case "not_favorite":
                    showNotFavorite = true;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Run a list of feed items through the filter.
     */
    public List<FeedItem> filter(List<FeedItem> items) {
        if(mProperties.length == 0) return items;

        List<FeedItem> result = new ArrayList<>();

        // Check for filter combinations that will always return an empty list
        // (e.g. requiring played and unplayed at the same time)
        if (showPlayed && showUnplayed) return result;
        if (showQueued && showNotQueued) return result;
        if (showDownloaded && showNotDownloaded) return result;

        final LongList queuedIds = DBReader.getQueueIDList();
        for (FeedItem item : items) {
            // If the item does not meet a requirement, skip it.

            if (showPlayed && !item.isPlayed()) continue;
            if (showUnplayed && item.isPlayed()) continue;

            if (showPaused && item.getState() != FeedItem.State.IN_PROGRESS) continue;
            if (showNotPaused && item.getState() == FeedItem.State.IN_PROGRESS) continue;

            boolean queued = queuedIds.contains(item.getId());
            if (showQueued && !queued) continue;
            if (showNotQueued && queued) continue;

            boolean downloaded = item.getMedia() != null && item.getMedia().isDownloaded();
            if (showDownloaded && !downloaded) continue;
            if (showNotDownloaded && downloaded) continue;

            if (showHasMedia && !item.hasMedia()) continue;
            if (showNoMedia && item.hasMedia()) continue;

            if (showIsFavorite && !item.isTagged(TAG_FAVORITE)) continue;
            if (showNotFavorite && item.isTagged(TAG_FAVORITE)) continue;

            // If the item reaches here, it meets all criteria
            result.add(item);
        }

        return result;
    }

    public String[] getValues() {
        return mProperties.clone();
    }

    public boolean isShowDownloaded() {
        return showDownloaded;
    }

}
