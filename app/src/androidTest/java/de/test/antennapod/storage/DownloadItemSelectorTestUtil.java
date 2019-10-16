package de.test.antennapod.storage;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType;

import static de.danoeh.antennapod.core.feed.FeedItem.NEW;
import static de.danoeh.antennapod.core.feed.FeedItem.PLAYED;
import static de.danoeh.antennapod.core.feed.FeedItem.UNPLAYED;

class DownloadItemSelectorTestUtil {
    private DownloadItemSelectorTestUtil() { }

    // Useful constants for readability

    static final boolean AUTO_DL_TRUE = true;
    static final boolean AUTO_DL_FALSE = false;
    static final boolean KEEP_UPDATED_TRUE = true;
    static final boolean KEEP_UPDATED_FALSE = false;

    //
    // Helpers to create test feeds. Callers still need to persist them.
    //

    @NonNull
    static Feed createFeed(int titleId, @NonNull SemanticType semanticType,
                           boolean isAutoDownload, String includeFilter, boolean isKeepUpdated,
                           FeedItem... feedItems) {
        return createFeed(FeedTestUtil.defaultFeedTitle(titleId), semanticType,
                isAutoDownload, includeFilter, isKeepUpdated, feedItems);
    }

    @NonNull
    static Feed createFeed(String title, @NonNull SemanticType semanticType,
                           boolean isAutoDownload, String includeFilter, boolean isKeepUpdated,
                           FeedItem... feedItems) {

        return FeedTestUtil.createFeed(title, feedPreferences -> {
            feedPreferences.setSemanticType(semanticType);
            feedPreferences.setKeepUpdated(isKeepUpdated);
            feedPreferences.setAutoDownload(isAutoDownload);
            feedPreferences.setFilter(new FeedFilter(includeFilter, ""));
        }, feedItems);
    }

    /**
     * @return a skeleton (incomplete) FeedItem of the specified state, createFeed() will fill in the details.
     */
    @NonNull
    static FeedItem cFI(int playState) {
        FeedItem item = FeedTestUtil.createFeedItemWithMedia();
        switch (playState) {
            case NEW:
                item.setNew();
                break;
            case UNPLAYED:
                item.setPlayed(false);
                break;
            case PLAYED:
                item.setPlayed(true);
                break;
            default:
                throw new IllegalArgumentException("Invalid playState: " + playState);
        }
        return item;
    }

}
