package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.feed.VorbisCommentChapter;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.comparator.DownloadStatusComparator;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.comparator.PlaybackCompletionDateComparator;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
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
     * Maximum size of the list returned by {@link #getPlaybackHistory(android.content.Context)}.
     */
    public static final int PLAYBACK_HISTORY_SIZE = 50;

    /**
     * Maximum size of the list returned by {@link #getDownloadLog(android.content.Context)}.
     */
    public static final int DOWNLOAD_LOG_SIZE = 200;


    private DBReader() {
    }

    /**
     * Returns a list of Feeds, sorted alphabetically by their title.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of Feeds, sorted alphabetically by their title. A Feed-object
     * of the returned list does NOT have its list of FeedItems yet. The FeedItem-list
     * can be loaded separately with {@link #getFeedItemList(android.content.Context, de.danoeh.antennapod.core.feed.Feed)}.
     */
    public static List<Feed> getFeedList(final Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting Feedlist");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<Feed> result = getFeedList(adapter);
        adapter.close();
        return result;
    }

    private static List<Feed> getFeedList(PodDBAdapter adapter) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting Feedlist");

        Cursor feedlistCursor = adapter.getAllFeedsCursor();
        List<Feed> feeds = new ArrayList<Feed>(feedlistCursor.getCount());

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
     * @param context A context that is used for opening the database connection.
     * @return A list of Strings with the download URLs of all feeds.
     */
    public static List<String> getFeedListDownloadUrls(final Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        List<String> result = new ArrayList<String>();
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
     * Returns a list of 'expired Feeds', i.e. Feeds that have not been updated for a certain amount of time.
     *
     * @param context        A context that is used for opening a database connection.
     * @param expirationTime Time that is used for determining whether a feed is outdated or not.
     *                       A Feed is considered expired if 'lastUpdate < (currentTime - expirationTime)' evaluates to true.
     * @return A list of Feeds, sorted alphabetically by their title. A Feed-object
     * of the returned list does NOT have its list of FeedItems yet. The FeedItem-list
     * can be loaded separately with {@link #getFeedItemList(android.content.Context, de.danoeh.antennapod.core.feed.Feed)}.
     */
    public static List<Feed> getExpiredFeedsList(final Context context, final long expirationTime) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("getExpiredFeedsList(%d)", expirationTime));

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor feedlistCursor = adapter.getExpiredFeedsCursor(expirationTime);
        List<Feed> feeds = new ArrayList<Feed>(feedlistCursor.getCount());

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
     * Takes a list of FeedItems and loads their corresponding Feed-objects from the database.
     * The feedID-attribute of a FeedItem must be set to the ID of its feed or the method will
     * not find the correct feed of an item.
     *
     * @param context A context that is used for opening a database connection.
     * @param items   The FeedItems whose Feed-objects should be loaded.
     */
    public static void loadFeedDataOfFeedItemlist(Context context,
                                                  List<FeedItem> items) {
        List<Feed> feeds = getFeedList(context);
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
     * used. In order to get information ABOUT the list of FeedItems, consider using {@link #getFeedStatisticsList(android.content.Context)} instead.
     *
     * @param context A context that is used for opening a database connection.
     * @param feed    The Feed whose items should be loaded
     * @return A list with the FeedItems of the Feed. The Feed-attribute of the FeedItems will already be set correctly.
     * The method does NOT change the items-attribute of the feed.
     */
    public static List<FeedItem> getFeedItemList(Context context,
                                                 final Feed feed) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting Feeditems of feed " + feed.getTitle());

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();

        Collections.sort(items, new FeedItemPubdateComparator());

        adapter.close();

        for (FeedItem item : items) {
            item.setFeed(feed);
        }

        return items;
    }

    static List<FeedItem> extractItemlistFromCursor(Context context, Cursor itemlistCursor) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<FeedItem> result = extractItemlistFromCursor(adapter, itemlistCursor);
        adapter.close();
        return result;
    }

    private static List<FeedItem> extractItemlistFromCursor(
            PodDBAdapter adapter, Cursor itemlistCursor) {
        ArrayList<String> itemIds = new ArrayList<String>();
        List<FeedItem> items = new ArrayList<FeedItem>(
                itemlistCursor.getCount());

        if (itemlistCursor.moveToFirst()) {
            do {
                long imageIndex = itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_IMAGE);
                FeedImage image = null;
                if (imageIndex != 0) {
                    image = getFeedImage(adapter, imageIndex);
                }

                FeedItem item = new FeedItem(itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_ID),
                        itemlistCursor.getString(PodDBAdapter.IDX_FI_SMALL_TITLE),
                        itemlistCursor.getString(PodDBAdapter.IDX_FI_SMALL_LINK),
                        new Date(itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_PUBDATE)),
                        itemlistCursor.getString(PodDBAdapter.IDX_FI_SMALL_PAYMENT_LINK),
                        itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_FEED),
                        new FlattrStatus(itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_FLATTR_STATUS)),
                        itemlistCursor.getInt(PodDBAdapter.IDX_FI_SMALL_HAS_CHAPTERS) > 0,
                        image,
                        (itemlistCursor.getInt(PodDBAdapter.IDX_FI_SMALL_READ) > 0),
                        itemlistCursor.getString(PodDBAdapter.IDX_FI_SMALL_ITEM_IDENTIFIER));

                itemIds.add(String.valueOf(item.getId()));

                items.add(item);
            } while (itemlistCursor.moveToNext());
        }

        extractMediafromItemlist(adapter, items, itemIds);
        return items;
    }

    private static void extractMediafromItemlist(PodDBAdapter adapter,
                                                 List<FeedItem> items, ArrayList<String> itemIds) {

        List<FeedItem> itemsCopy = new ArrayList<FeedItem>(items);
        Cursor cursor = adapter.getFeedMediaCursorByItemID(itemIds
                .toArray(new String[itemIds.size()]));
        if (cursor.moveToFirst()) {
            do {
                long itemId = cursor.getLong(PodDBAdapter.KEY_MEDIA_FEEDITEM_INDEX);
                // find matching feed item
                FeedItem item = getMatchingItemForMedia(itemId, itemsCopy);
                if (item != null) {
                    item.setMedia(extractFeedMediaFromCursorRow(cursor));
                    item.getMedia().setItem(item);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private static FeedMedia extractFeedMediaFromCursorRow(final Cursor cursor) {
        long mediaId = cursor.getLong(PodDBAdapter.KEY_ID_INDEX);
        Date playbackCompletionDate = null;
        long playbackCompletionTime = cursor
                .getLong(PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE_INDEX);
        if (playbackCompletionTime > 0) {
            playbackCompletionDate = new Date(
                    playbackCompletionTime);
        }

        return new FeedMedia(
                mediaId,
                null,
                cursor.getInt(PodDBAdapter.KEY_DURATION_INDEX),
                cursor.getInt(PodDBAdapter.KEY_POSITION_INDEX),
                cursor.getLong(PodDBAdapter.KEY_SIZE_INDEX),
                cursor.getString(PodDBAdapter.KEY_MIME_TYPE_INDEX),
                cursor.getString(PodDBAdapter.KEY_FILE_URL_INDEX),
                cursor.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX),
                cursor.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0,
                playbackCompletionDate,
                cursor.getInt(PodDBAdapter.KEY_PLAYED_DURATION_INDEX));
    }

    private static Feed extractFeedFromCursorRow(PodDBAdapter adapter,
                                                 Cursor cursor) {
        Date lastUpdate = new Date(
                cursor.getLong(PodDBAdapter.IDX_FEED_SEL_STD_LASTUPDATE));

        final FeedImage image;
        long imageIndex = cursor.getLong(PodDBAdapter.IDX_FEED_SEL_STD_IMAGE);
        if (imageIndex != 0) {
            image = getFeedImage(adapter, imageIndex);
        } else {
            image = null;
        }
        Feed feed = new Feed(cursor.getLong(PodDBAdapter.IDX_FEED_SEL_STD_ID),
                lastUpdate,
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_TITLE),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_LINK),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_DESCRIPTION),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_PAYMENT_LINK),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_AUTHOR),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_LANGUAGE),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_TYPE),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_FEED_IDENTIFIER),
                image,
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_FILE_URL),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_DOWNLOAD_URL),
                cursor.getInt(PodDBAdapter.IDX_FEED_SEL_STD_DOWNLOADED) > 0,
                new FlattrStatus(cursor.getLong(PodDBAdapter.IDX_FEED_SEL_STD_FLATTR_STATUS)),
                cursor.getInt(PodDBAdapter.IDX_FEED_SEL_STD_IS_PAGED) > 0,
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_STD_NEXT_PAGE_LINK));

        if (image != null) {
            image.setOwner(feed);
        }

        FeedPreferences preferences = new FeedPreferences(cursor.getLong(PodDBAdapter.IDX_FEED_SEL_STD_ID),
                cursor.getInt(PodDBAdapter.IDX_FEED_SEL_PREFERENCES_AUTO_DOWNLOAD) > 0,
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_PREFERENCES_USERNAME),
                cursor.getString(PodDBAdapter.IDX_FEED_SEL_PREFERENCES_PASSWORD));

        feed.setPreferences(preferences);
        return feed;
    }


    private static FeedItem getMatchingItemForMedia(long itemId,
                                                    List<FeedItem> items) {
        for (FeedItem item : items) {
            if (item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    static List<FeedItem> getQueue(Context context, PodDBAdapter adapter) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting queue");

        Cursor itemlistCursor = adapter.getQueueCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();
        loadFeedDataOfFeedItemlist(context, items);

        return items;
    }

    /**
     * Loads the IDs of the FeedItems in the queue. This method should be preferred over
     * {@link #getQueue(android.content.Context)} if the FeedItems of the queue are not needed.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of IDs sorted by the same order as the queue. The caller can wrap the returned
     * list in a {@link de.danoeh.antennapod.core.util.QueueAccess} object for easier access to the queue's properties.
     */
    public static List<Long> getQueueIDList(Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);

        adapter.open();
        List<Long> result = getQueueIDList(adapter);
        adapter.close();

        return result;
    }

    static List<Long> getQueueIDList(PodDBAdapter adapter) {
        adapter.open();
        Cursor queueCursor = adapter.getQueueIDCursor();

        List<Long> queueIds = new ArrayList<Long>(queueCursor.getCount());
        if (queueCursor.moveToFirst()) {
            do {
                queueIds.add(queueCursor.getLong(0));
            } while (queueCursor.moveToNext());
        }
        return queueIds;
    }


    /**
     * Loads a list of the FeedItems in the queue. If the FeedItems of the queue are not used directly, consider using
     * {@link #getQueueIDList(android.content.Context)} instead.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of FeedItems sorted by the same order as the queue. The caller can wrap the returned
     * list in a {@link de.danoeh.antennapod.core.util.QueueAccess} object for easier access to the queue's properties.
     */
    public static List<FeedItem> getQueue(Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting queue");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<FeedItem> items = getQueue(context, adapter);
        adapter.close();
        return items;
    }

    /**
     * Loads a list of FeedItems whose episode has been downloaded.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of FeedItems whose episdoe has been downloaded.
     */
    public static List<FeedItem> getDownloadedItems(Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting downloaded items");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor itemlistCursor = adapter.getDownloadedItemsCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();
        loadFeedDataOfFeedItemlist(context, items);
        Collections.sort(items, new FeedItemPubdateComparator());

        adapter.close();
        return items;

    }

    /**
     * Loads a list of FeedItems whose 'read'-attribute is set to false.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of FeedItems whose 'read'-attribute it set to false. If the FeedItems in the list are not used,
     * consider using {@link #getUnreadItemIds(android.content.Context)} instead.
     */
    public static List<FeedItem> getUnreadItemsList(Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting unread items list");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor itemlistCursor = adapter.getUnreadItemsCursor();
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();

        loadFeedDataOfFeedItemlist(context, items);

        adapter.close();

        return items;
    }

    /**
     * Loads the IDs of the FeedItems whose 'read'-attribute is set to false.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of IDs of the FeedItems whose 'read'-attribute is set to false. This method should be preferred
     * over {@link #getUnreadItemsList(android.content.Context)} if the FeedItems in the UnreadItems list are not used.
     */
    public static long[] getUnreadItemIds(Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getUnreadItemIdsCursor();
        long[] itemIds = new long[cursor.getCount()];
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                itemIds[i] = cursor.getLong(PodDBAdapter.KEY_ID_INDEX);
                i++;
            } while (cursor.moveToNext());
        }
        return itemIds;
    }


    /**
     * Loads a list of FeedItems sorted by pubDate in descending order.
     *
     * @param context A context that is used for opening a database connection.
     * @param limit   The maximum number of episodes that should be loaded.
     */
    public static List<FeedItem> getRecentlyPublishedEpisodes(Context context, int limit) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting recently published items list");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor itemlistCursor = adapter.getRecentlyPublishedItemsCursor(limit);
        List<FeedItem> items = extractItemlistFromCursor(adapter,
                itemlistCursor);
        itemlistCursor.close();

        loadFeedDataOfFeedItemlist(context, items);

        adapter.close();

        return items;
    }

    /**
     * Loads the playback history from the database. A FeedItem is in the playback history if playback of the correpsonding episode
     * has been completed at least once.
     *
     * @param context A context that is used for opening a database connection.
     * @return The playback history. The FeedItems are sorted by their media's playbackCompletionDate in descending order.
     * The size of the returned list is limited by {@link #PLAYBACK_HISTORY_SIZE}.
     */
    public static List<FeedItem> getPlaybackHistory(final Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Loading playback history");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Cursor mediaCursor = adapter.getCompletedMediaCursor(PLAYBACK_HISTORY_SIZE);
        String[] itemIds = new String[mediaCursor.getCount()];
        for (int i = 0; i < itemIds.length && mediaCursor.moveToPosition(i); i++) {
            itemIds[i] = Long.toString(mediaCursor.getLong(PodDBAdapter.KEY_MEDIA_FEEDITEM_INDEX));
        }
        mediaCursor.close();
        Cursor itemCursor = adapter.getFeedItemCursor(itemIds);
        List<FeedItem> items = extractItemlistFromCursor(adapter, itemCursor);
        loadFeedDataOfFeedItemlist(context, items);
        itemCursor.close();
        adapter.close();

        Collections.sort(items, new PlaybackCompletionDateComparator());
        return items;
    }

    /**
     * Loads the download log from the database.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list with DownloadStatus objects that represent the download log.
     * The size of the returned list is limited by {@link #DOWNLOAD_LOG_SIZE}.
     */
    public static List<DownloadStatus> getDownloadLog(Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Extracting DownloadLog");

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor logCursor = adapter.getDownloadLogCursor(DOWNLOAD_LOG_SIZE);
        List<DownloadStatus> downloadLog = new ArrayList<DownloadStatus>(
                logCursor.getCount());

        if (logCursor.moveToFirst()) {
            do {
                long id = logCursor.getLong(PodDBAdapter.KEY_ID_INDEX);

                long feedfileId = logCursor
                        .getLong(PodDBAdapter.KEY_FEEDFILE_INDEX);
                int feedfileType = logCursor
                        .getInt(PodDBAdapter.KEY_FEEDFILETYPE_INDEX);
                boolean successful = logCursor
                        .getInt(PodDBAdapter.KEY_SUCCESSFUL_INDEX) > 0;
                int reason = logCursor.getInt(PodDBAdapter.KEY_REASON_INDEX);
                String reasonDetailed = logCursor
                        .getString(PodDBAdapter.KEY_REASON_DETAILED_INDEX);
                String title = logCursor
                        .getString(PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE_INDEX);
                Date completionDate = new Date(
                        logCursor
                                .getLong(PodDBAdapter.KEY_COMPLETION_DATE_INDEX)
                );
                downloadLog.add(new DownloadStatus(id, title, feedfileId,
                        feedfileType, successful, DownloadError.fromCode(reason), completionDate,
                        reasonDetailed));

            } while (logCursor.moveToNext());
        }
        logCursor.close();
        Collections.sort(downloadLog, new DownloadStatusComparator());
        return downloadLog;
    }

    /**
     * Loads the FeedItemStatistics objects of all Feeds in the database. This method should be preferred over
     * {@link #getFeedItemList(android.content.Context, de.danoeh.antennapod.core.feed.Feed)} if only metadata about
     * the FeedItems is needed.
     *
     * @param context A context that is used for opening a database connection.
     * @return A list of FeedItemStatistics objects sorted alphabetically by their Feed's title.
     */
    public static List<FeedItemStatistics> getFeedStatisticsList(final Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<FeedItemStatistics> result = new ArrayList<FeedItemStatistics>();
        Cursor cursor = adapter.getFeedStatisticsCursor();
        if (cursor.moveToFirst()) {
            do {
                result.add(new FeedItemStatistics(cursor.getLong(PodDBAdapter.IDX_FEEDSTATISTICS_FEED),
                        cursor.getInt(PodDBAdapter.IDX_FEEDSTATISTICS_NUM_ITEMS),
                        cursor.getInt(PodDBAdapter.IDX_FEEDSTATISTICS_NEW_ITEMS),
                        cursor.getInt(PodDBAdapter.IDX_FEEDSTATISTICS_IN_PROGRESS_EPISODES),
                        new Date(cursor.getLong(PodDBAdapter.IDX_FEEDSTATISTICS_LATEST_EPISODE))));
            } while (cursor.moveToNext());
        }

        cursor.close();
        adapter.close();
        return result;
    }

    /**
     * Loads a specific Feed from the database.
     *
     * @param context A context that is used for opening a database connection.
     * @param feedId  The ID of the Feed
     * @return The Feed or null if the Feed could not be found. The Feeds FeedItems will also be loaded from the
     * database and the items-attribute will be set correctly.
     */
    public static Feed getFeed(final Context context, final long feedId) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Feed result = getFeed(context, feedId, adapter);
        adapter.close();
        return result;
    }

    static Feed getFeed(final Context context, final long feedId, PodDBAdapter adapter) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Loading feed with id " + feedId);
        Feed feed = null;

        Cursor feedCursor = adapter.getFeedCursor(feedId);
        if (feedCursor.moveToFirst()) {
            feed = extractFeedFromCursorRow(adapter, feedCursor);
            feed.setItems(getFeedItemList(context, feed));
        } else {
            Log.e(TAG, "getFeed could not find feed with id " + feedId);
        }
        feedCursor.close();
        return feed;
    }

    static FeedItem getFeedItem(final Context context, final long itemId, PodDBAdapter adapter) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Loading feeditem with id " + itemId);
        FeedItem item = null;

        Cursor itemCursor = adapter.getFeedItemCursor(Long.toString(itemId));
        if (itemCursor.moveToFirst()) {
            List<FeedItem> list = extractItemlistFromCursor(adapter, itemCursor);
            if (list.size() > 0) {
                item = list.get(0);
                loadFeedDataOfFeedItemlist(context, list);
                if (item.hasChapters()) {
                    loadChaptersOfFeedItem(adapter, item);
                }
            }
        }
        return item;

    }

    /**
     * Loads a specific FeedItem from the database. This method should not be used for loading more
     * than one FeedItem because this method might query the database several times for each item.
     *
     * @param context A context that is used for opening a database connection.
     * @param itemId  The ID of the FeedItem
     * @return The FeedItem or null if the FeedItem could not be found. All FeedComponent-attributes
     * as well as chapter marks of the FeedItem will also be loaded from the database.
     */
    public static FeedItem getFeedItem(final Context context, final long itemId) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Loading feeditem with id " + itemId);

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        FeedItem item = getFeedItem(context, itemId, adapter);
        adapter.close();
        return item;

    }

    /**
     * Loads additional information about a FeedItem, e.g. shownotes
     *
     * @param context A context that is used for opening a database connection.
     * @param item    The FeedItem
     */
    public static void loadExtraInformationOfFeedItem(final Context context, final FeedItem item) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor extraCursor = adapter.getExtraInformationOfItem(item);
        if (extraCursor.moveToFirst()) {
            String description = extraCursor
                    .getString(PodDBAdapter.IDX_FI_EXTRA_DESCRIPTION);
            String contentEncoded = extraCursor
                    .getString(PodDBAdapter.IDX_FI_EXTRA_CONTENT_ENCODED);
            item.setDescription(description);
            item.setContentEncoded(contentEncoded);
        }
        adapter.close();
    }

    /**
     * Loads the list of chapters that belongs to this FeedItem if available. This method overwrites
     * any chapters that this FeedItem has. If no chapters were found in the database, the chapters
     * reference of the FeedItem will be set to null.
     *
     * @param context A context that is used for opening a database connection.
     * @param item    The FeedItem
     */
    public static void loadChaptersOfFeedItem(final Context context, final FeedItem item) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        loadChaptersOfFeedItem(adapter, item);
        adapter.close();
    }

    static void loadChaptersOfFeedItem(PodDBAdapter adapter, FeedItem item) {
        Cursor chapterCursor = adapter
                .getSimpleChaptersOfFeedItemCursor(item);
        if (chapterCursor.moveToFirst()) {
            item.setChapters(new ArrayList<Chapter>());
            do {
                int chapterType = chapterCursor
                        .getInt(PodDBAdapter.KEY_CHAPTER_TYPE_INDEX);
                Chapter chapter = null;
                long start = chapterCursor
                        .getLong(PodDBAdapter.KEY_CHAPTER_START_INDEX);
                String title = chapterCursor
                        .getString(PodDBAdapter.KEY_TITLE_INDEX);
                String link = chapterCursor
                        .getString(PodDBAdapter.KEY_CHAPTER_LINK_INDEX);

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
                    chapter.setId(chapterCursor
                            .getLong(PodDBAdapter.KEY_ID_INDEX));
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
     * @param context A context that is used for opening a database connection.
     * @return The number of downloaded episodes.
     */
    public static int getNumberOfDownloadedEpisodes(final Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        final int result = adapter.getNumberOfDownloadedEpisodes();
        adapter.close();
        return result;
    }

    /**
     * Returns the number of unread items.
     *
     * @param context A context that is used for opening a database connection.
     * @return The number of unread items.
     */
    public static int getNumberOfUnreadItems(final Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        final int result = adapter.getNumberOfUnreadItems();
        adapter.close();
        return result;
    }

    /**
     * Searches the DB for a FeedImage of the given id.
     *
     * @param context A context that is used for opening a database connection.
     * @param imageId The id of the object
     * @return The found object
     */
    public static FeedImage getFeedImage(final Context context, final long imageId) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        FeedImage result = getFeedImage(adapter, imageId);
        adapter.close();
        return result;
    }

    /**
     * Searches the DB for a FeedImage of the given id.
     *
     * @param id The id of the object
     * @return The found object
     */
    static FeedImage getFeedImage(PodDBAdapter adapter, final long id) {
        Cursor cursor = adapter.getImageCursor(id);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            return null;
        }
        FeedImage image = new FeedImage(id, cursor.getString(cursor
                .getColumnIndex(PodDBAdapter.KEY_TITLE)),
                cursor.getString(cursor
                        .getColumnIndex(PodDBAdapter.KEY_FILE_URL)),
                cursor.getString(cursor
                        .getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL)),
                cursor.getInt(cursor
                        .getColumnIndex(PodDBAdapter.KEY_DOWNLOADED)) > 0
        );
        cursor.close();
        return image;
    }

    /**
     * Searches the DB for a FeedMedia of the given id.
     *
     * @param context A context that is used for opening a database connection.
     * @param mediaId The id of the object
     * @return The found object
     */
    public static FeedMedia getFeedMedia(final Context context, final long mediaId) {
        PodDBAdapter adapter = new PodDBAdapter(context);

        adapter.open();
        Cursor mediaCursor = adapter.getSingleFeedMediaCursor(mediaId);

        FeedMedia media = null;
        if (mediaCursor.moveToFirst()) {
            final long itemId = mediaCursor.getLong(PodDBAdapter.KEY_MEDIA_FEEDITEM_INDEX);
            media = extractFeedMediaFromCursorRow(mediaCursor);
            FeedItem item = getFeedItem(context, itemId);
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
     * @param context A context that is used for opening a database connection.
     * @return The flattr queue as a List.
     */
    public static List<FlattrThing> getFlattrQueue(Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<FlattrThing> result = new ArrayList<FlattrThing>();

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
     * Returns true if the flattr queue is empty.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static boolean getFlattrQueueEmpty(Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        boolean empty = adapter.getFlattrQueueSize() == 0;
        adapter.close();
        return empty;
    }

    /**
     * Returns data necessary for displaying the navigation drawer. This includes
     * the list of subscriptions, the number of items in the queue and the number of unread
     * items.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static NavDrawerData getNavDrawerData(Context context) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        List<Feed> feeds = getFeedList(adapter);
        int queueSize = adapter.getQueueSize();
        int numUnreadItems = adapter.getNumberOfUnreadItems();
        NavDrawerData result = new NavDrawerData(feeds, queueSize, numUnreadItems);
        adapter.close();
        return result;
    }

    public static class NavDrawerData {
        public List<Feed> feeds;
        public int queueSize;
        public int numUnreadItems;

        public NavDrawerData(List<Feed> feeds, int queueSize, int numUnreadItems) {
            this.feeds = feeds;
            this.queueSize = queueSize;
            this.numUnreadItems = numUnreadItems;
        }
    }
}
