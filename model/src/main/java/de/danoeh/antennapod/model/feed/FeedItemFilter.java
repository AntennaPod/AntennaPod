package de.danoeh.antennapod.model.feed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeedItemFilter implements Serializable {

    private final String[] properties;

    public final boolean showPlayed;
    public final boolean showUnplayed;
    public final boolean showPaused;
    public final boolean showNotPaused;
    public final boolean showNew;
    public final boolean showQueued;
    public final boolean showNotQueued;
    public final boolean showDownloaded;
    public final boolean showNotDownloaded;
    public final boolean showHasMedia;
    public final boolean showNoMedia;
    public final boolean showIsFavorite;
    public final boolean showNotFavorite;
    public final boolean showInHistory;
    public final boolean includeSubscribed;
    public final boolean includeArchived;
    public final boolean includeNotSubscribed;

    public static final String PLAYED = "played";
    public static final String UNPLAYED = "unplayed";
    public static final String NEW = "new";
    public static final String PAUSED = "paused";
    public static final String NOT_PAUSED = "not_paused";
    public static final String IS_FAVORITE = "is_favorite";
    public static final String NOT_FAVORITE = "not_favorite";
    public static final String HAS_MEDIA = "has_media";
    public static final String NO_MEDIA = "no_media";
    public static final String QUEUED = "queued";
    public static final String NOT_QUEUED = "not_queued";
    public static final String DOWNLOADED = "downloaded";
    public static final String NOT_DOWNLOADED = "not_downloaded";
    public static final String IS_IN_HISTORY = "is_in_history";
    public static final String INCLUDE_SUBSCRIBED = "include_subscribed";
    public static final String INCLUDE_ARCHIVED = "include_archived";
    public static final String INCLUDE_NOT_SUBSCRIBED = "include_not_subscribed";
    public static final String INCLUDE_ALL_FEED_STATES =
            INCLUDE_SUBSCRIBED + "," + INCLUDE_ARCHIVED + "," + INCLUDE_NOT_SUBSCRIBED;

    public static FeedItemFilter unfiltered() {
        return new FeedItemFilter();
    }

    public FeedItemFilter(String properties) {
        this(properties.isEmpty() ? new String[0] : properties.split(","));
    }

    public FeedItemFilter(FeedItemFilter filter, String... additionalProperties) {
        this(String.join(",", filter.getValues()) + "," + String.join(",", additionalProperties));
    }

    public FeedItemFilter(String... properties) {
        String joined = String.join(",", properties);
        this.properties = joined.isEmpty() ? new String[0] : joined.split(",");

        // see R.arrays.feed_filter_values
        showUnplayed = hasProperty(UNPLAYED);
        showPaused = hasProperty(PAUSED);
        showNotPaused = hasProperty(NOT_PAUSED);
        showPlayed = hasProperty(PLAYED);
        showQueued = hasProperty(QUEUED);
        showNotQueued = hasProperty(NOT_QUEUED);
        showDownloaded = hasProperty(DOWNLOADED);
        showNotDownloaded = hasProperty(NOT_DOWNLOADED);
        showHasMedia = hasProperty(HAS_MEDIA);
        showNoMedia = hasProperty(NO_MEDIA);
        showIsFavorite = hasProperty(IS_FAVORITE);
        showNotFavorite = hasProperty(NOT_FAVORITE);
        showNew = hasProperty(NEW);
        showInHistory = hasProperty(IS_IN_HISTORY);
        includeSubscribed = hasProperty(INCLUDE_SUBSCRIBED);
        includeArchived = hasProperty(INCLUDE_ARCHIVED);
        includeNotSubscribed = hasProperty(INCLUDE_NOT_SUBSCRIBED);
    }

    private boolean hasProperty(String property) {
        return Arrays.asList(properties).contains(property);
    }

    public String[] getValues() {
        return properties.clone();
    }

    public List<String> getValuesList() {
        return Arrays.asList(properties);
    }

    public FeedItemFilter without(String property) {
        List<String> newValues = new ArrayList<>(Arrays.asList(properties));
        newValues.remove(property);
        return new FeedItemFilter(newValues.toArray(new String[0]));
    }

    public boolean matches(FeedItem item) {
        if (showNew && !item.isNew()) {
            return false;
        } else if (showPlayed && !item.isPlayed()) {
            return false;
        } else if (showUnplayed && item.isPlayed()) {
            return false;
        } else if (showPaused && !item.isInProgress()) {
            return false;
        } else if (showNotPaused && item.isInProgress()) {
            return false;
        } else if (showNew && !item.isNew()) {
            return false;
        } else if (showQueued && !item.isTagged(FeedItem.TAG_QUEUE)) {
            return false;
        } else if (showNotQueued && item.isTagged(FeedItem.TAG_QUEUE)) {
            return false;
        } else if (showDownloaded && !item.isDownloaded()) {
            return false;
        } else if (showNotDownloaded && item.isDownloaded()) {
            return false;
        } else if (showHasMedia && !item.hasMedia()) {
            return false;
        } else if (showNoMedia && item.hasMedia()) {
            return false;
        } else if (showIsFavorite && !item.isTagged(FeedItem.TAG_FAVORITE)) {
            return false;
        } else if (showNotFavorite && item.isTagged(FeedItem.TAG_FAVORITE)) {
            return false;
        } else if (showInHistory && item.getMedia() != null
                && item.getMedia().getLastPlayedTimeHistory().getTime() == 0) {
            return false;
        }
        if (item.getFeed() != null) {
            int state = item.getFeed().getState();
            if (includeSubscribed || includeArchived || includeNotSubscribed) {
                if (state == Feed.STATE_SUBSCRIBED && !includeSubscribed) {
                    return false;
                } else if (state == Feed.STATE_ARCHIVED && !includeArchived) {
                    return false;
                } else if (state == Feed.STATE_NOT_SUBSCRIBED && !includeNotSubscribed) {
                    return false;
                }
            } else if (state != Feed.STATE_SUBSCRIBED) {
                return false;
            }
        }
        return true;
    }
}
