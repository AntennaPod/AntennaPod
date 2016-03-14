package de.danoeh.antennapod.core.storage;

import android.database.Cursor;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.feed.VorbisCommentChapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.util.LongIntMap;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.comparator.DownloadStatusComparator;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.comparator.PlaybackCompletionDateComparator;
import de.danoeh.antennapod.core.util.flattr.FlattrThing;

/**
 * Provides methods for reading data from the AntennaPod database.
 * In general, all database calls in DBReader-methods are executed on the caller's thread.
 * This means that the caller should make sure that DBReader-methods are not executed on the GUI-thread.
 * This class will use the {@link de.danoeh.antennapod.core.feed.EventDistributor} to notify listeners about changes in the database.
 */
public final class DBReader {

    private static final String TAG = "DBReader";

    /**
     * Maximum size of the list returned by {@link #getPlaybackHistory()}.
     */
    public static final int PLAYBACK_HISTORY_SIZE = 50;

    /**
     * Maximum size of the list returned by {@link #getDownloadLog()}.
     */
    public static final int DOWNLOAD_LOG_SIZE = 200;


    private DBReader() {
    }

    /**
     * Returns a list of Feeds, sorted alphabetically by their title.
     *
     * @return A list of Feeds, sorted alphabetically by their title. A Feed-object
     * of the returned list does NOT have its list of FeedItems yet. The FeedItem-list
     * can be loaded separately with {@link #getFeedItemList(Feed)}.
     */
    public static List<Feed> getFeedList() {
        Log.d(TAG, "Extracting Feedlist");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<Feed> result = getFeedList(adapter);
        adapter.close();
        return result;
    }

    private static List<Feed> getFeedList(PodDBAdapter adapter) {
        Cursor feedlistCursor = adapter.getAllFeedsCursor();
        List<Feed> feeds = new ArrayList<>(feedlistCursor.getCount());

        if (feedlistCursor.moveToFirst()) {
            do {
                Feed feed = extractFeedFromCursorRow(adapter, feedlistCursor);
                feeds.add(feed);
            } while (feedlistCursor.moveToNext());
        }
        feedlistCursor.close();
        return feeds;
    }

    /**
     * Returns a list with the download URLs of all feeds.
     *
     * @return A list of Strings with the download URLs of all feeds.
     */
    public static List<String> getFeedListDownloadUrls() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        List<String> result = new ArrayList<>();
        adapter.open();
        Cursor feeds = adapter.getFeedCursorDownloadUrls();
        if (feeds.moveToFirst()) {
            do {
                result.add(feeds.getString(1));
            } while (feeds.moveToNext());
        }
        feeds.close();
        adapter.close();

        return result;
    }


    /**
     * Loads additional data in to the feed items from other database queries
     * @param items the FeedItems who should have other data loaded
     */
    public static void loadAdditionalFeedItemListData(List<FeedItem> items) {
        loadTagsOfFeedItemList(items);
        loadFeedDataOfFeedItemList(items);
    }

    public static void loadTagsOfFeedItemList(List<FeedItem> items) {
        LongList favoriteIds = getFavoriteIDList();
        LongList queueIds = getQueueIDList();

        for (FeedItem item : items) {
            if (favoriteIds.contains(item.getId())) {
                item.addTag(FeedItem.TAG_FAVORITE);
            }
            if (queueIds.contains(item.getId())) {
                item.addTag(FeedItem.TAG_QUEUE);
            }
        }
    }

    /**
     * Takes a list of FeedItems and loads their corresponding Feed-objects from the database.
     * The feedID-attribute of a FeedItem must be set to the ID of its feed or the method will
     * not find the correct feed of an item.
     *
     * @param items   The FeedItems whose Feed-objects should be loaded.
     */
    public static void loadFeedDataOfFeedItemList(List<FeedItem> items) {
        List<Feed> feeds = getFeedList();
        for (FeedItem item : items) {
            for (Feed feed : feeds) {
                if (feed.getId() == item.getFeedId()) {
                    item.setFeed(feed);
                    break;
                }
            }
            if (item.getFeed() == null) {
                Log.w(TAG, "No match found for item with ID " + item.getId() + ". Feed ID was " + item.getFeedId());
            }
        }
    }

    /**
     * Loads the list of FeedItems for a certain Feed-object. This method should NOT be used if the FeedItems are not
     * used. In order to get information ABOUT the list of FeedItems, consider using {@link #getFeedStatisticsList()} instead.
     *
     * @param feed    The Feed whose items should be loaded
     * @return A list with the FeedItems of the Feed. The Feed-attribute of the FeedItems will already be set correctly.
     * The method does NOT change the items-attribute of the feed.
     */
    public static List<FeedItem> getFeedItemList(final Feed feed) {
        Log.d(TAG, "getFeedItemList() called with: " + "feed = [" + feed + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();
        adapter.close();

        Collections.sort(items, new FeedItemPubdateComparator());

        for (FeedItem item : items) {
            item.setFeed(feed);
        }

        return items;
    }

    public static List<FeedItem> extractItemlistFromCursor(Cursor itemlistCursor) {
        Log.d(TAG, "extractItemlistFromCursor() called with: " + "itemlistCursor = [" + itemlistCursor + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<FeedItem> result = extractItemlistFromCursor(adapter, itemlistCursor);
        adapter.close();
        return result;
    }

    private static List<FeedItem> extractItemlistFromCursor(PodDBAdapter adapter,
                                                            Cursor cursor) {
        List<FeedItem> result = new ArrayList<>(cursor.getCount());

        LongList imageIds = new LongList(cursor.getCount());
        LongList itemIds = new LongList(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                int indexImage = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE);
                long imageId = cursor.getLong(indexImage);
                imageIds.add(imageId);

                FeedItem item = FeedItem.fromCursor(cursor);
                result.add(item);
                itemIds.add(item.getId());
            } while (cursor.moveToNext());
            Map<Long,FeedImage> images = getFeedImages(adapter, imageIds.toArray());
            Map<Long,FeedMedia> medias = getFeedMedia(adapter, itemIds.toArray());
            for(int i=0; i < result.size(); i++) {
                FeedItem item = result.get(i);
                long imageId = imageIds.get(i);
                FeedImage image = images.get(imageId);
                item.setImage(image);
                FeedMedia media = medias.get(item.getId());
                item.setMedia(media);
                if(media != null) {
                    media.setItem(item);
                }
            }
        }
        return result;
    }

    private static Map<Long,FeedMedia> getFeedMedia(PodDBAdapter adapter,
                                                    long... itemIds) {

        String[] ids = new String[itemIds.length];
        for(int i=0, len=itemIds.length; i < len; i++) {
            ids[i] = String.valueOf(itemIds[i]);
        }
        Map<Long,FeedMedia> result = new ArrayMap<>(itemIds.length);
        Cursor cursor = adapter.getFeedMediaCursor(ids);
        try {
            if (cursor.moveToFirst()) {
                do {
                    int index = cursor.getColumnIndex(PodDBAdapter.KEY_FEEDITEM);
                    long itemId = cursor.getLong(index);
                    FeedMedia media = FeedMedia.fromCursor(cursor);
                    result.put(itemId, media);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private static Feed extractFeedFromCursorRow(PodDBAdapter adapter,
                                                 Cursor cursor) {
        final FeedImage image;
        int indexImage = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE);
        long imageId = cursor.getLong(indexImage);
        if (imageId != 0) {
            image = getFeedImage(adapter, imageId);
        } else {
            image = null;
        }

        Feed feed = Feed.fromCursor(cursor);
        if (image != null) {
            feed.setImage(image);
            image.setOwner(feed);
        }

        FeedPreferences preferences = FeedPreferences.fromCursor(cursor);
        feed.setPreferences(preferences);

        return feed;
    }

    static List<FeedItem> getQueue(PodDBAdapter adapter) {
        Log.d(TAG, "getQueue()");
        Cursor itemlistCursor = adapter.getQueueCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemlistCursor);
        itemlistCursor.close();
        loadAdditionalFeedItemListData(items);
        return items;
    }

    /**
     * Loads the IDs of the FeedItems in the queue. This method should be preferred over
     * {@link #getQueue()} if the FeedItems of the queue are not needed.
     *
     * @return A list of IDs sorted by the same order as the queue. The caller can wrap the returned
     * list in a {@link de.danoeh.antennapod.core.util.QueueAccess} object for easier access to the queue's properties.
     */
    public static LongList getQueueIDList() {
        Log.d(TAG, "getQueueIDList() called");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        LongList result = getQueueIDList(adapter);
        adapter.close();
        return result;
    }

    static LongList getQueueIDList(PodDBAdapter adapter) {
        Cursor queueCursor = adapter.getQueueIDCursor();

        LongList queueIds = new LongList(queueCursor.getCount());
        if (queueCursor.moveToFirst()) {
            do {
                queueIds.add(queueCursor.getLong(0));
            } while (queueCursor.moveToNext());
        }
        queueCursor.close();
        return queueIds;
    }

    /**
     * Loads a list of the FeedItems in the queue. If the FeedItems of the queue are not used directly, consider using
     * {@link #getQueueIDList()} instead.
     *
     * @return A list of FeedItems sorted by the same order as the queue. The caller can wrap the returned
     * list in a {@link de.danoeh.antennapod.core.util.QueueAccess} object for easier access to the queue's properties.
     */
    public static List<FeedItem> getQueue() {
        Log.d(TAG, "getQueue() called with: " + "");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<FeedItem> items = getQueue(adapter);
        adapter.close();
        return items;
    }

    /**
     * Loads a list of FeedItems whose episode has been downloaded.
     *
     * @return A list of FeedItems whose episdoe has been downloaded.
     */
    public static List<FeedItem> getDownloadedItems() {
        Log.d(TAG, "getDownloadedItems() called with: " + "");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor itemlistCursor = adapter.getDownloadedItemsCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();
        loadAdditionalFeedItemListData(items);
        adapter.close();

        Collections.sort(items, new FeedItemPubdateComparator());

        return items;

    }

    /**
     * Loads a list of FeedItems whose 'read'-attribute is set to false.
     *
     * @return A list of FeedItems whose 'read'-attribute it set to false.
     */
    public static List<FeedItem> getUnreadItemsList() {
        Log.d(TAG, "getUnreadItemsList() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor itemlistCursor = adapter.getUnreadItemsCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemlistCursor);
        itemlistCursor.close();

        loadAdditionalFeedItemListData(items);

        adapter.close();

        return items;
    }

    /**
     * Loads a list of FeedItems that are considered new.
     * Excludes items from feeds that do not have keep updated enabled.
     * @return A list of FeedItems that are considered new.
     */
    public static List<FeedItem> getNewItemsList() {
        Log.d(TAG, "getNewItemsList() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor itemlistCursor = adapter.getNewItemsCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemlistCursor);
        itemlistCursor.close();

        loadAdditionalFeedItemListData(items);

        adapter.close();

        return items;
    }

    public static List<FeedItem> getFavoriteItemsList() {
        Log.d(TAG, "getFavoriteItemsList() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor itemlistCursor = adapter.getFavoritesCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemlistCursor);
        itemlistCursor.close();

        loadAdditionalFeedItemListData(items);

        adapter.close();

        return items;
    }

    public static LongList getFavoriteIDList() {
        Log.d(TAG, "getFavoriteIDList() called");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor favoritesCursor = adapter.getFavoritesCursor();

        LongList favoriteIDs = new LongList(favoritesCursor.getCount());
        if (favoritesCursor.moveToFirst()) {
            do {
                favoriteIDs.add(favoritesCursor.getLong(0));
            } while (favoritesCursor.moveToNext());
        }
        favoritesCursor.close();
        adapter.close();
        return favoriteIDs;
    }

    /**
     * Loads a list of FeedItems sorted by pubDate in descending order.
     *
     * @param limit   The maximum number of episodes that should be loaded.
     */
    public static List<FeedItem> getRecentlyPublishedEpisodes(int limit) {
        Log.d(TAG, "getRecentlyPublishedEpisodes() called with: " + "limit = [" + limit + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor itemlistCursor = adapter.getRecentlyPublishedItemsCursor(limit);
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemlistCursor);
        itemlistCursor.close();

        loadAdditionalFeedItemListData(items);

        adapter.close();

        return items;
    }

    /**
     * Loads the playback history from the database. A FeedItem is in the playback history if playback of the correpsonding episode
     * has been completed at least once.
     *
     * @return The playback history. The FeedItems are sorted by their media's playbackCompletionDate in descending order.
     * The size of the returned list is limited by {@link #PLAYBACK_HISTORY_SIZE}.
     */
    public static List<FeedItem> getPlaybackHistory() {
        Log.d(TAG, "getPlaybackHistory() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Cursor mediaCursor = adapter.getCompletedMediaCursor(PLAYBACK_HISTORY_SIZE);
        String[] itemIds = new String[mediaCursor.getCount()];
        for (int i = 0; i < itemIds.length && mediaCursor.moveToPosition(i); i++) {
            int index = mediaCursor.getColumnIndex(PodDBAdapter.KEY_FEEDITEM);
            itemIds[i] = Long.toString(mediaCursor.getLong(index));
        }
        mediaCursor.close();
        Cursor itemCursor = adapter.getFeedItemCursor(itemIds);
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemCursor);
        loadAdditionalFeedItemListData(items);
        itemCursor.close();
        adapter.close();

        Collections.sort(items, new PlaybackCompletionDateComparator());
        return items;
    }

    /**
     * Loads the download log from the database.
     *
     * @return A list with DownloadStatus objects that represent the download log.
     * The size of the returned list is limited by {@link #DOWNLOAD_LOG_SIZE}.
     */
    public static List<DownloadStatus> getDownloadLog() {
        Log.d(TAG, "getDownloadLog() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor logCursor = adapter.getDownloadLogCursor(DOWNLOAD_LOG_SIZE);
        List<DownloadStatus> downloadLog = new ArrayList<>(logCursor.getCount());

        if (logCursor.moveToFirst()) {
            do {
                DownloadStatus status = DownloadStatus.fromCursor(logCursor);
                downloadLog.add(status);
            } while (logCursor.moveToNext());
        }
        logCursor.close();
        adapter.close();
        Collections.sort(downloadLog, new DownloadStatusComparator());
        return downloadLog;
    }

    /**
     * Loads the download log for a particular feed from the database.
     *
     * @param feed Feed for which the download log is loaded
     * @return A list with DownloadStatus objects that represent the feed's download log,
     *         newest events first.
     */
    public static List<DownloadStatus> getFeedDownloadLog(Feed feed) {
        Log.d(TAG, "getFeedDownloadLog() called with: " + "feed = [" + feed + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getDownloadLog(Feed.FEEDFILETYPE_FEED, feed.getId());
        List<DownloadStatus> downloadLog = new ArrayList<>(cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                DownloadStatus status = DownloadStatus.fromCursor(cursor);
                downloadLog.add(status);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.close();
        Collections.sort(downloadLog, new DownloadStatusComparator());
        return downloadLog;
    }

    /**
     * Loads the FeedItemStatistics objects of all Feeds in the database. This method should be preferred over
     * {@link #getFeedItemList(Feed)} if only metadata about
     * the FeedItems is needed.
     *
     * @return A list of FeedItemStatistics objects sorted alphabetically by their Feed's title.
     */
    public static List<FeedItemStatistics> getFeedStatisticsList() {
        Log.d(TAG, "getFeedStatisticsList() called");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<FeedItemStatistics> result = new ArrayList<>();
        Cursor cursor = adapter.getFeedStatisticsCursor();
        if (cursor.moveToFirst()) {
            do {
                FeedItemStatistics fis = FeedItemStatistics.fromCursor(cursor);
                result.add(fis);
            } while (cursor.moveToNext());
        }

        cursor.close();
        adapter.close();
        return result;
    }

    /**
     * Loads a specific Feed from the database.
     *
     * @param feedId  The ID of the Feed
     * @return The Feed or null if the Feed could not be found. The Feeds FeedItems will also be loaded from the
     * database and the items-attribute will be set correctly.
     */
    public static Feed getFeed(final long feedId) {
        Log.d(TAG, "getFeed() called with: " + "feedId = [" + feedId + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Feed result = getFeed(feedId, adapter);
        adapter.close();
        return result;
    }

    static Feed getFeed(final long feedId, PodDBAdapter adapter) {
        Feed feed = null;

        Cursor feedCursor = adapter.getFeedCursor(feedId);
        if (feedCursor.moveToFirst()) {
            feed = extractFeedFromCursorRow(adapter, feedCursor);
            feed.setItems(getFeedItemList(feed));
        } else {
            Log.e(TAG, "getFeed could not find feed with id " + feedId);
        }
        feedCursor.close();
        return feed;
    }

    static FeedItem getFeedItem(final long itemId, PodDBAdapter adapter) {
        Log.d(TAG, "Loading feeditem with id " + itemId);
        FeedItem item = null;

        Cursor itemCursor = adapter.getFeedItemCursor(Long.toString(itemId));
        if (itemCursor.moveToFirst()) {
            List<FeedItem> list = extractItemlistFromCursor(adapter, itemCursor);
            if (list.size() > 0) {
                item = list.get(0);
                loadAdditionalFeedItemListData(list);
                if (item.hasChapters()) {
                    loadChaptersOfFeedItem(adapter, item);
                }
            }
        }
        itemCursor.close();
        return item;
    }

    static List<FeedItem> getFeedItems(PodDBAdapter adapter, final long... itemIds) {

        String[] ids = new String[itemIds.length];
        for(int i = 0; i < itemIds.length; i++) {
            long itemId = itemIds[i];
            ids[i] = Long.toString(itemId);
        }

        List<FeedItem> result;

        Cursor itemCursor = adapter.getFeedItemCursor(ids);
        if (itemCursor.moveToFirst()) {
            result = extractItemlistFromCursor(adapter, itemCursor);
            loadAdditionalFeedItemListData(result);
            for(FeedItem item : result) {
                if (item.hasChapters()) {
                    loadChaptersOfFeedItem(adapter, item);
                }
            }
        } else {
            result = Collections.emptyList();
        }
        itemCursor.close();
        return result;

    }

    /**
     * Loads a specific FeedItem from the database. This method should not be used for loading more
     * than one FeedItem because this method might query the database several times for each item.
     *
     * @param itemId  The ID of the FeedItem
     * @return The FeedItem or null if the FeedItem could not be found. All FeedComponent-attributes
     * as well as chapter marks of the FeedItem will also be loaded from the database.
     */
    public static FeedItem getFeedItem(final long itemId) {
        Log.d(TAG, "getFeedItem() called with: " + "itemId = [" + itemId + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        FeedItem item = getFeedItem(itemId, adapter);
        adapter.close();
        return item;
    }

    static FeedItem getFeedItem(final String podcastUrl, final String episodeUrl, PodDBAdapter adapter) {
        Log.d(TAG, "Loading feeditem with podcast url " + podcastUrl + " and episode url " + episodeUrl);
        FeedItem item = null;
        Cursor itemCursor = adapter.getFeedItemCursor(podcastUrl, episodeUrl);
        if (itemCursor.moveToFirst()) {
            List<FeedItem> list = extractItemlistFromCursor(adapter, itemCursor);
            if (list.size() > 0) {
                item = list.get(0);
                loadAdditionalFeedItemListData(list);
                if (item.hasChapters()) {
                    loadChaptersOfFeedItem(adapter, item);
                }
            }
        }
        itemCursor.close();
        return item;
    }

    /**
     * Loads specific FeedItems from the database. This method canbe used for loading more
     * than one FeedItem
     *
     * @param itemIds  The IDs of the FeedItems
     * @return The FeedItems or an empty list if none of the FeedItems could be found. All FeedComponent-attributes
     * as well as chapter marks of the FeedItems will also be loaded from the database.
     */
    public static List<FeedItem> getFeedItems(final long... itemIds) {
        Log.d(TAG, "getFeedItems() called with: " + "itemIds = [" + Arrays.toString(itemIds) + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<FeedItem> items = getFeedItems(adapter, itemIds);
        adapter.close();
        return items;
    }


    /**
     * Returns credentials based on image URL
     *
     * @param imageUrl  The URL of the image
     * @return Credentials in format "<Username>:<Password>", empty String if no authorization given
     */
    public static String getImageAuthentication(final String imageUrl) {
        Log.d(TAG, "getImageAuthentication() called with: " + "imageUrl = [" + imageUrl + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        String credentials = getImageAuthentication(imageUrl, adapter);
        adapter.close();
        return credentials;

    }

    static String getImageAuthentication(final String imageUrl, PodDBAdapter adapter) {
        String credentials = null;
        Cursor cursor = adapter.getImageAuthenticationCursor(imageUrl);
        try {
            if (cursor.moveToFirst()) {
                String username = cursor.getString(0);
                String password = cursor.getString(1);
                if(username != null && password != null) {
                    credentials = username + ":" + password;
                } else {
                    credentials = "";
                }
            } else {
                credentials = "";
            }
        } finally {
            cursor.close();
        }
        return credentials;
    }

    /**
     * Loads a specific FeedItem from the database.
     *
     * @param podcastUrl the corresponding feed's url
     * @param episodeUrl the feed item's url
     * @return The FeedItem or null if the FeedItem could not be found. All FeedComponent-attributes
     * as well as chapter marks of the FeedItem will also be loaded from the database.
     */
    public static FeedItem getFeedItem(final String podcastUrl, final String episodeUrl) {
        Log.d(TAG, "getFeedItem() called with: " + "podcastUrl = [" + podcastUrl + "], episodeUrl = [" + episodeUrl + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        FeedItem item = getFeedItem(podcastUrl, episodeUrl, adapter);
        adapter.close();
        return item;
    }

    /**
     * Loads additional information about a FeedItem, e.g. shownotes
     *
     * @param item    The FeedItem
     */
    public static void loadExtraInformationOfFeedItem(final FeedItem item) {
        Log.d(TAG, "loadExtraInformationOfFeedItem() called with: " + "item = [" + item + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor extraCursor = adapter.getExtraInformationOfItem(item);
        if (extraCursor.moveToFirst()) {
            int indexDescription = extraCursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION);
            String description = extraCursor.getString(indexDescription);
            int indexContentEncoded = extraCursor.getColumnIndex(PodDBAdapter.KEY_CONTENT_ENCODED);
            String contentEncoded = extraCursor.getString(indexContentEncoded);
            item.setDescription(description);
            item.setContentEncoded(contentEncoded);
        }
        extraCursor.close();
        adapter.close();
    }

    /**
     * Loads the list of chapters that belongs to this FeedItem if available. This method overwrites
     * any chapters that this FeedItem has. If no chapters were found in the database, the chapters
     * reference of the FeedItem will be set to null.
     *
     * @param item    The FeedItem
     */
    public static void loadChaptersOfFeedItem(final FeedItem item) {
        Log.d(TAG, "loadChaptersOfFeedItem() called with: " + "item = [" + item + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        loadChaptersOfFeedItem(adapter, item);
        adapter.close();
    }

    static void loadChaptersOfFeedItem(PodDBAdapter adapter, FeedItem item) {
        Cursor chapterCursor = adapter.getSimpleChaptersOfFeedItemCursor(item);
        if (chapterCursor.moveToFirst()) {
            item.setChapters(new ArrayList<>());
            do {
                int indexType = chapterCursor.getColumnIndex(PodDBAdapter.KEY_CHAPTER_TYPE);
                int indexStart = chapterCursor.getColumnIndex(PodDBAdapter.KEY_START);
                int indexTitle = chapterCursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
                int indexLink = chapterCursor.getColumnIndex(PodDBAdapter.KEY_LINK);

                int chapterType = chapterCursor.getInt(indexType);
                Chapter chapter = null;
                long start = chapterCursor.getLong(indexStart);
                String title = chapterCursor.getString(indexTitle);
                String link = chapterCursor.getString(indexLink);

                switch (chapterType) {
                    case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
                        chapter = new SimpleChapter(start, title, item,
                                link);
                        break;
                    case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
                        chapter = new ID3Chapter(start, title, item,
                                link);
                        break;
                    case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
                        chapter = new VorbisCommentChapter(start,
                                title, item, link);
                        break;
                }
                if (chapter != null) {
                    int indexId = chapterCursor.getColumnIndex(PodDBAdapter.KEY_ID);
                    chapter.setId(chapterCursor.getLong(indexId));
                    item.getChapters().add(chapter);
                }
            } while (chapterCursor.moveToNext());
        } else {
            item.setChapters(null);
        }
        chapterCursor.close();
    }

    /**
     * Returns the number of downloaded episodes.
     *
     * @return The number of downloaded episodes.
     */
    public static int getNumberOfDownloadedEpisodes() {
        Log.d(TAG, "getNumberOfDownloadedEpisodes() called with: " + "");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        final int result = adapter.getNumberOfDownloadedEpisodes();
        adapter.close();
        return result;
    }

    /**
     * Searches the DB for a FeedImage of the given id.
     *
     * @param imageId The id of the object
     * @return The found object
     */
    public static FeedImage getFeedImage(final long imageId) {
        Log.d(TAG, "getFeedImage() called with: " + "imageId = [" + imageId + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        FeedImage result = getFeedImage(adapter, imageId);
        adapter.close();
        return result;
    }

    /**
     * Searches the DB for a FeedImage of the given id.
     *
     * @param imageId The id of the object
     * @return The found object
     */
    private static FeedImage getFeedImage(PodDBAdapter adapter, final long imageId) {
        return getFeedImages(adapter, imageId).get(imageId);
    }

    /**
     * Searches the DB for a FeedImage of the given id.
     *
     * @param imageIds The ids of the images
     * @return Map that associates the id of an image with the image itself
     */
    private static Map<Long,FeedImage> getFeedImages(PodDBAdapter adapter, final long... imageIds) {
        String[] ids = new String[imageIds.length];
        for(int i=0, len=imageIds.length; i < len; i++) {
            ids[i] = String.valueOf(imageIds[i]);
        }
        Cursor cursor = adapter.getImageCursor(ids);
        Map<Long, FeedImage> result = new ArrayMap<>(cursor.getCount());
        try {
            if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
                return Collections.emptyMap();
            }
            do {
                FeedImage image = FeedImage.fromCursor(cursor);
                result.put(image.getId(), image);
            } while(cursor.moveToNext());
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * Searches the DB for a FeedMedia of the given id.
     *
     * @param mediaId The id of the object
     * @return The found object
     */
    public static FeedMedia getFeedMedia(final long mediaId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();

        adapter.open();
        Cursor mediaCursor = adapter.getSingleFeedMediaCursor(mediaId);

        FeedMedia media = null;
        if (mediaCursor.moveToFirst()) {
            int indexFeedItem = mediaCursor.getColumnIndex(PodDBAdapter.KEY_FEEDITEM);
            final long itemId = mediaCursor.getLong(indexFeedItem);
            media = FeedMedia.fromCursor(mediaCursor);
            FeedItem item = getFeedItem(itemId);
            if (media != null && item != null) {
                media.setItem(item);
                item.setMedia(media);
            }
        }

        mediaCursor.close();
        adapter.close();

        return media;
    }

    /**
     * Returns the flattr queue as a List of FlattrThings. The list consists of Feeds and FeedItems.
     *
     * @return The flattr queue as a List.
     */
    public static List<FlattrThing> getFlattrQueue() {
        Log.d(TAG, "getFlattrQueue() called with: " + "");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<FlattrThing> result = new ArrayList<>();

        // load feeds
        Cursor feedCursor = adapter.getFeedsInFlattrQueueCursor();
        if (feedCursor.moveToFirst()) {
            do {
                result.add(extractFeedFromCursorRow(adapter, feedCursor));
            } while (feedCursor.moveToNext());
        }
        feedCursor.close();

        //load feed items
        Cursor feedItemCursor = adapter.getFeedItemsInFlattrQueueCursor();
        result.addAll(extractItemlistFromCursor(adapter, feedItemCursor));
        feedItemCursor.close();

        adapter.close();
        Log.d(TAG, "Returning flattrQueueIterator for queue with " + result.size() + " items.");
        return result;
    }

    /**
     * Returns data necessary for displaying the navigation drawer. This includes
     * the list of subscriptions, the number of items in the queue and the number of unread
     * items.
     *
     */
    public static NavDrawerData getNavDrawerData() {
        Log.d(TAG, "getNavDrawerData() called with: " + "");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<Feed> feeds = getFeedList(adapter);
        long[] feedIds = new long[feeds.size()];
        for(int i=0; i < feeds.size(); i++) {
            feedIds[i] = feeds.get(i).getId();
        }
        final LongIntMap feedCounters = adapter.getFeedCounters(feedIds);

        Comparator<Feed> comparator;
        int feedOrder = UserPreferences.getFeedOrder();
        if(feedOrder == UserPreferences.FEED_ORDER_COUNTER) {
            comparator = (lhs, rhs) -> {
                long counterLhs = feedCounters.get(lhs.getId());
                long counterRhs = feedCounters.get(rhs.getId());
                if(counterLhs > counterRhs) {
                    // reverse natural order: podcast with most unplayed episodes first
                    return -1;
                } else if(counterLhs == counterRhs) {
                    return lhs.getTitle().compareTo(rhs.getTitle());
                } else {
                    return 1;
                }
            };
        } else if(feedOrder == UserPreferences.FEED_ORDER_ALPHABETICAL) {
            comparator = (lhs, rhs) -> {
                String t1 = lhs.getTitle();
                String t2 = rhs.getTitle();
                if(t1 == null) {
                    return 1;
                } else if(t2 == null) {
                    return -1;
                } else {
                    return t1.toLowerCase().compareTo(t2.toLowerCase());
                }
            };
        } else {
            comparator = (lhs, rhs) -> {
                if(lhs.getItems() == null || lhs.getItems().size() == 0) {
                    List<FeedItem> items = DBReader.getFeedItemList(lhs);
                    lhs.setItems(items);
                }
                if(rhs.getItems() == null || rhs.getItems().size() == 0) {
                    List<FeedItem> items = DBReader.getFeedItemList(rhs);
                    rhs.setItems(items);
                }
                if(lhs.getMostRecentItem() == null) {
                    return 1;
                } else if(rhs.getMostRecentItem() == null) {
                    return -1;
                } else {
                    Date d1 = lhs.getMostRecentItem().getPubDate();
                    Date d2 = rhs.getMostRecentItem().getPubDate();
                    return d2.compareTo(d1);
                }
            };
        }

        Collections.sort(feeds, comparator);
        int queueSize = adapter.getQueueSize();
        int numNewItems = adapter.getNumberOfNewItems();
        int numDownloadedItems = adapter.getNumberOfDownloadedEpisodes();

        NavDrawerData result = new NavDrawerData(feeds, queueSize, numNewItems, numDownloadedItems,
                feedCounters, UserPreferences.getEpisodeCleanupAlgorithm().getReclaimableItems());
        adapter.close();
        return result;
    }

    public static class NavDrawerData {
        public List<Feed> feeds;
        public int queueSize;
        public int numNewItems;
        public int numDownloadedItems;
        public LongIntMap feedCounters;
        public int reclaimableSpace;

        public NavDrawerData(List<Feed> feeds,
                             int queueSize,
                             int numNewItems,
                             int numDownloadedItems,
                             LongIntMap feedIndicatorValues,
                             int reclaimableSpace) {
            this.feeds = feeds;
            this.queueSize = queueSize;
            this.numNewItems = numNewItems;
            this.numDownloadedItems = numDownloadedItems;
            this.feedCounters = feedIndicatorValues;
            this.reclaimableSpace = reclaimableSpace;
        }
    }
}
