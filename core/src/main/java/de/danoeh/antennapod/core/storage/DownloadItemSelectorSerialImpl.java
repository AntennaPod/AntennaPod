package de.danoeh.antennapod.core.storage;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class DownloadItemSelectorSerialImpl implements DownloadItemSelector {
    private static final String TAG = "DlItemSelectorSerial";

    public DownloadItemSelectorSerialImpl() { }

    @NonNull
    @Override
    public List<? extends FeedItem> getAutoDownloadableEpisodes() {
        // - OPEN: accept an input hint for hint of how many are needed max (to return early)

        // Outline:
        // - starting with the feed of which the id is the next one
        // - repeat until all feeds (not in queue) ar tried
        //   (OPEN: repeat until the required number of items are in the result, if a hint is given)
        //   - add the next unplayed episode to the result (i.e., the one after the last played / in-playback episode)
        //   - then use the next feed
        // - return the result

        List<Feed> serialFeedsByDownloadOrder = getSerialFeedsOrderedByDownloadOrder();

        Set<Long> feedIdsWithDownloadedMedia = new ArraySet<>();
        for(FeedItem item : DBReader.getDownloadedItems()) {
            feedIdsWithDownloadedMedia.add(item.getFeedId());
        }

        List<FeedItem> candidatesLater = new ArrayList<>();
        List<FeedItem> candidates = new ArrayList<>(serialFeedsByDownloadOrder .size());

        for (Feed feed : serialFeedsByDownloadOrder) {
            FeedItem item = getNextItemToDownloadForSerial(feed);
            if (item != null) {
                if (feedIdsWithDownloadedMedia.contains(item.getFeedId())) {
                    candidatesLater.add(item);
                } else {
                    candidates.add(item);
                }
            }
        }
        // push items whose feed already has something downloaded to the end
        candidates.addAll(candidatesLater);
        return candidates;
    }

    @VisibleForTesting
    public List<Feed> getSerialFeedsOrderedByDownloadOrder() {
        // - get list of all serial feeds grouped ordered by ID
        List<? extends Feed> serialFeedsById = getSerialFeedsOrderedById();
        int numFeeds = serialFeedsById.size();
        // - find out serial feed id with of which item is the last completely played or in-playback
        FeedItem lastPlayedSerialItem = getLastPlayedSerialFeedItem();

        List<Feed> serialFeedsByDownloadOrder = new ArrayList<>(numFeeds);
        if (lastPlayedSerialItem == null) {
            serialFeedsByDownloadOrder.addAll(serialFeedsById);
        } else {
            int idxFeedLastPlayed = -1;
            for (int i = 0; i < numFeeds; i++) {
                Feed feed = serialFeedsById.get(i);
                if (lastPlayedSerialItem.getFeedId() == feed.getId()) {
                    idxFeedLastPlayed = i;
                }
            }
            if (idxFeedLastPlayed < 0) {
                Log.e(TAG, "getSerialFeedsOrderedByDownloadOrder: assertion error (idxFeedLastPlayed < 0)"
                        + " , fallback to using order by feed id");
                serialFeedsByDownloadOrder.addAll(serialFeedsById);
            } else {
                int idxStart = (idxFeedLastPlayed + 1) % numFeeds;
                serialFeedsByDownloadOrder.addAll(serialFeedsById.subList(idxStart, numFeeds));
                // round-robin: go back to the beginning
                if (idxStart > 0) {
                    serialFeedsByDownloadOrder.addAll(serialFeedsById.subList(0, idxStart));
                }
            }
        }
        return serialFeedsByDownloadOrder;
    }

    @VisibleForTesting
    public List<Feed> getSerialFeedsOrderedById() {
        List<Feed> serialFeeds = new ArrayList<>();
        for (Feed feed: DBReader.getFeedList()) {
            if (SemanticType.SERIAL == feed.getPreferences().getSemanticType()) {
                feed.setItems(DBReader.getFeedItemList(feed));
                serialFeeds.add(feed);
            }
        }

        Collections.sort(serialFeeds, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

        return serialFeeds;
    }

    @VisibleForTesting
    @Nullable
    public FeedItem getLastPlayedSerialFeedItem() {
        return DBReader.getLatestSerialPlaybackItem();
    }

    @VisibleForTesting
    @Nullable
    public FeedItem getNextItemToDownloadForSerial(@NonNull Feed feed) {
        if (SemanticType.SERIAL != feed.getPreferences().getSemanticType()) {
            throw new IllegalArgumentException("getNextItemToDownloadForSerial() - supplied feed is not serial");
        }

        // feedItems sorted by pubDate ascending
        List<FeedItem> feedItems = new ArrayList<>(feed.getItems());
        Collections.sort(feedItems, FeedItemPubdateComparator.ascending);

        if (feedItems.size() < 1) {
            return null;
        }

        int idxLatestPlayedOrInProgress = -1;
        for(int i = feedItems.size() - 1; i >= 0; i--) {
            FeedItem item = feedItems.get(i);
            FeedMedia media = item.getMedia();
            if (item.isPlayed() ||
                    (media != null && media.getLastPlayedTime() > 0)) {
                idxLatestPlayedOrInProgress = i;
                break;
            }
        }

        if (idxLatestPlayedOrInProgress < 0) {
            // all unplayed, return the oldest, non-downloaded ones
            return firstNonDownloadedItem(feedItems, 0);
        } else if (idxLatestPlayedOrInProgress >= feedItems.size() - 1) {
            // the latest one is played or in progress, so nothing
            return null;
        } else {
            return firstNonDownloadedItem(feedItems, idxLatestPlayedOrInProgress + 1);
        }
    }

    @Nullable
    private FeedItem firstNonDownloadedItem(List<? extends FeedItem> feedItems, int startIdx) {
        for(int i = startIdx; i < feedItems.size(); i++) {
            FeedItem fi = feedItems.get(i);
            FeedMedia media = fi.getMedia();
            if (media != null && !media.isDownloaded()) {
                return fi;
            }
        }
        return null;
    }
}
