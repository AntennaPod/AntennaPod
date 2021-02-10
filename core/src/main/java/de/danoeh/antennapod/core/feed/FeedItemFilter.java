package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LongList;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;

public class FeedItemFilter {

    private final String[] properties;

    public final boolean showPlayed;
    public final boolean showUnplayed;
    public final boolean showPaused;
    public final boolean showNotPaused;
    public final boolean showQueued;
    public final boolean showNotQueued;
    public final boolean showDownloaded;
    public final boolean showNotDownloaded;
    public final boolean showHasMedia;
    public final boolean showNoMedia;
    public final boolean showIsFavorite;
    public final boolean showNotFavorite;

    public static FeedItemFilter unfiltered() {
        return new FeedItemFilter("");
    }

    public FeedItemFilter(String properties) {
        this(TextUtils.split(properties, ","));
    }

    public FeedItemFilter(String[] properties) {
        this.properties = properties;

        // see R.arrays.feed_filter_values
        showUnplayed = hasProperty("unplayed");
        showPaused = hasProperty("paused");
        showNotPaused = hasProperty("not_paused");
        showPlayed = hasProperty("played");
        showQueued = hasProperty("queued");
        showNotQueued = hasProperty("not_queued");
        showDownloaded = hasProperty("downloaded");
        showNotDownloaded = hasProperty("not_downloaded");
        showHasMedia = hasProperty("has_media");
        showNoMedia = hasProperty("no_media");
        showIsFavorite = hasProperty("is_favorite");
        showNotFavorite = hasProperty("not_favorite");
    }

    private boolean hasProperty(String property) {
        return Arrays.asList(properties).contains(property);
    }

    /**
     * Run a list of feed items through the filter.
     */
    public List<FeedItem> filter(List<FeedItem> items) {
        if (properties.length == 0) {
            return items;
        }

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
        return properties.clone();
    }

    public boolean isShowDownloaded() {
        return showDownloaded;
    }
}
