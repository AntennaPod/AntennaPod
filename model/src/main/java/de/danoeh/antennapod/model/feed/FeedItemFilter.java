package de.danoeh.antennapod.model.feed;

import android.text.TextUtils;
import java.util.Arrays;

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

    public String[] getValues() {
        return properties.clone();
    }

    public boolean isShowDownloaded() {
        return showDownloaded;
    }
}
