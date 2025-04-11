package de.danoeh.antennapod.storage.database;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedOrder;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.storage.database.mapper.ChapterCursor;
import de.danoeh.antennapod.storage.database.mapper.DownloadResultCursor;
import de.danoeh.antennapod.storage.database.mapper.FeedCursor;
import de.danoeh.antennapod.storage.database.mapper.FeedItemCursor;

/**
 * Provides methods for reading data from the AntennaPod database.
 * In general, all database calls in DBReader-methods are executed on the caller's thread.
 * This means that the caller should make sure that DBReader-methods are not executed on the GUI-thread.
 */
public final class DBReader {

    private static final String TAG = "DBReader";

    /**
     * Maximum size of the list returned by {@link #getDownloadLog()}.
     */
    private static final int DOWNLOAD_LOG_SIZE = 200;


    private DBReader() {
    }

    /**
     * Returns a list of Feeds, sorted alphabetically by their title.
     *
     * @return A list of Feeds, sorted alphabetically by their title.
     *      A Feed-object of the returned list does NOT have its list of FeedItems yet.
     *      The FeedItem-list can be loaded separately with getFeedItemList().
     */
    @NonNull
    public static List<Feed> getFeedList() {
        Log.d(TAG, "Extracting Feedlist");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedCursor cursor = new FeedCursor(adapter.getAllFeedsCursor())) {
            List<Feed> feeds = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                feeds.add(cursor.getFeed());
            }
            return feeds;
        } finally {
            adapter.close();
        }
    }

    /**
     * Returns a list with the download URLs of all feeds.
     *
     * @return A list of Strings with the download URLs of all feeds.
     */
    public static List<String> getFeedListDownloadUrls() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getFeedCursorDownloadUrls()) {
            List<String> result = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String url = cursor.getString(1);
                if (url != null && !url.startsWith(Feed.PREFIX_LOCAL_FOLDER)) {
                    result.add(url);
                }
            }
            return result;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads additional data in to the feed items from other database queries
     *
     * @param items the FeedItems who should have other data loaded
     */
    public static void loadAdditionalFeedItemListData(List<FeedItem> items) {
        loadTagsOfFeedItemList(items);
        loadFeedDataOfFeedItemList(items);
    }

    private static void loadTagsOfFeedItemList(List<FeedItem> items) {
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
     * @param items The FeedItems whose Feed-objects should be loaded.
     */
    private static void loadFeedDataOfFeedItemList(List<FeedItem> items) {
        List<Feed> feeds = getFeedList();

        Map<Long, Feed> feedIndex = new ArrayMap<>(feeds.size());
        for (Feed feed : feeds) {
            feedIndex.put(feed.getId(), feed);
        }
        for (FeedItem item : items) {
            Feed feed = feedIndex.get(item.getFeedId());
            if (feed == null) {
                Log.w(TAG, "No match found for item with ID " + item.getId() + ". Feed ID was " + item.getFeedId());
                feed = new Feed("", "", "Error: Item without feed");
            }
            item.setFeed(feed);
        }
    }

    /**
     * Loads the list of FeedItems for a certain Feed-object.
     * This method should NOT be used if the FeedItems are not used.
     *
     * @param feed The Feed whose items should be loaded
     * @return A list with the FeedItems of the Feed. The Feed-attribute of the FeedItems will already be set correctly.
     */
    public static List<FeedItem> getFeedItemList(final Feed feed, final FeedItemFilter filter, SortOrder sortOrder,
                                                 int offset, int limit) {
        Log.d(TAG, "getFeedItemList() called with: " + "feed = [" + feed + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getItemsOfFeedCursor(
                feed, filter, sortOrder, offset, limit))) {
            List<FeedItem> items = extractItemlistFromCursor(cursor);
            feed.setItems(items);
            for (FeedItem item : items) {
                item.setFeed(feed);
            }
            return items;
        } finally {
            adapter.close();
        }
    }

    @NonNull
    private static List<FeedItem> extractItemlistFromCursor(FeedItemCursor cursor) {
        List<FeedItem> result = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            result.add(cursor.getFeedItem());
        }
        return result;
    }

    /**
     * Loads the IDs of the FeedItems in the queue. This method should be preferred over
     * {@link #getQueue()} if the FeedItems of the queue are not needed.
     *
     * @return A list of IDs sorted by the same order as the queue.
     */
    public static LongList getQueueIDList() {
        Log.d(TAG, "getQueueIDList() called");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getQueueIDCursor()) {
            LongList queueIds = new LongList(cursor.getCount());
            while (cursor.moveToNext()) {
                queueIds.add(cursor.getLong(0));
            }
            return queueIds;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a list of the FeedItems in the queue. If the FeedItems of the queue are not used directly, consider using
     * {@link #getQueueIDList()} instead.
     *
     * @return A list of FeedItems sorted by the same order as the queue.
     */
    @NonNull
    public static List<FeedItem> getQueue() {
        Log.d(TAG, "getQueue() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getQueueCursor())) {
            List<FeedItem> items = extractItemlistFromCursor(cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    private static LongList getFavoriteIDList() {
        Log.d(TAG, "getFavoriteIDList() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getFavoritesIdsCursor()) {
            LongList favoriteIDs = new LongList(cursor.getCount());
            while (cursor.moveToNext()) {
                favoriteIDs.add(cursor.getLong(0));
            }
            return favoriteIDs;
        } finally {
            adapter.close();
        }
    }

    /**
     *
     * @param offset The first episode that should be loaded.
     * @param limit The maximum number of episodes that should be loaded.
     * @param filter The filter describing which episodes to filter out.
     */
    @NonNull
    public static List<FeedItem> getEpisodes(int offset, int limit, FeedItemFilter filter, SortOrder sortOrder) {
        Log.d(TAG, "getRecentlyPublishedEpisodes() called with: offset=" + offset + ", limit=" + limit);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getEpisodesCursor(offset, limit, filter, sortOrder))) {
            List<FeedItem> items = extractItemlistFromCursor(cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    public static int getTotalEpisodeCount(FeedItemFilter filter) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getEpisodeCountCursor(filter)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return -1;
        } finally {
            adapter.close();
        }
    }

    public static int getFeedEpisodeCount(long feedId, FeedItemFilter filter) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getFeedEpisodeCountCursor(feedId, filter)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return -1;
        } finally {
            adapter.close();
        }
    }

    public static List<FeedItem> getRandomEpisodes(int limit, int seed) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getRandomEpisodesCursor(limit, seed))) {
            List<FeedItem> items = extractItemlistFromCursor(cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the download log from the database.
     *
     * @return A list with DownloadStatus objects that represent the download log.
     * The size of the returned list is limited by {@link #DOWNLOAD_LOG_SIZE}.
     */
    public static List<DownloadResult> getDownloadLog() {
        Log.d(TAG, "getDownloadLog() called");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (DownloadResultCursor cursor = new DownloadResultCursor(adapter.getDownloadLogCursor(DOWNLOAD_LOG_SIZE))) {
            List<DownloadResult> downloadLog = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                downloadLog.add(cursor.getDownloadResult());
            }
            return downloadLog;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the download log for a particular feed from the database.
     *
     * @param feedId Feed id for which the download log is loaded
     * @return A list with DownloadStatus objects that represent the feed's download log,
     * newest events first.
     */
    public static List<DownloadResult> getFeedDownloadLog(long feedId) {
        Log.d(TAG, "getFeedDownloadLog() called with: " + "feed = [" + feedId + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (DownloadResultCursor cursor = new DownloadResultCursor(
                adapter.getDownloadLog(Feed.FEEDFILETYPE_FEED, feedId))) {
            List<DownloadResult> downloadLog = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                downloadLog.add(cursor.getDownloadResult());
            }
            return downloadLog;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a specific Feed from the database.
     *
     * @param feedId The ID of the Feed
     * @param filtered <code>true</code> if only the visible items should be loaded according to the feed filter.
     * @return The Feed or null if the Feed could not be found. The Feeds FeedItems will also be loaded from the
     *         database and the items-attribute will be set correctly.
     */
    @Nullable
    public static Feed getFeed(final long feedId, boolean filtered, int offset, int limit) {
        Log.d(TAG, "getFeed() called with: " + "feedId = [" + feedId + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Feed feed = null;
        try (FeedCursor cursor = new FeedCursor(adapter.getFeedCursor(feedId))) {
            if (cursor.moveToNext()) {
                feed = cursor.getFeed();
                FeedItemFilter filter = (filtered && feed.getItemFilter() != null)
                        ? feed.getItemFilter() : FeedItemFilter.unfiltered();
                filter = new FeedItemFilter(filter, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED);
                List<FeedItem> items = getFeedItemList(feed, filter, feed.getSortOrder(), offset, limit);
                for (FeedItem item : items) {
                    item.setFeed(feed);
                }
                loadTagsOfFeedItemList(items);
                feed.setItems(items);
            } else {
                Log.e(TAG, "getFeed could not find feed with id " + feedId);
            }
            return feed;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a specific FeedItem from the database. This method should not be used for loading more
     * than one FeedItem because this method might query the database several times for each item.
     *
     * @param itemId The ID of the FeedItem
     * @return The FeedItem or null if the FeedItem could not be found.
     */
    @Nullable
    public static FeedItem getFeedItem(final long itemId) {
        Log.d(TAG, "getFeedItem() called with: " + "itemId = [" + itemId + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getFeedItemCursor(Long.toString(itemId)))) {
            List<FeedItem> list = extractItemlistFromCursor(cursor);
            if (!list.isEmpty()) {
                FeedItem item = list.get(0);
                loadAdditionalFeedItemListData(list);
                return item;
            }
        } finally {
            adapter.close();
        }
        return null;
    }

    /**
     * Get next feed item in queue following a particular feeditem
     *
     * @param item The FeedItem
     * @return The FeedItem next in queue or null if the FeedItem could not be found.
     */
    @Nullable
    public static FeedItem getNextInQueue(FeedItem item) {
        Log.d(TAG, "getNextInQueue() called with: " + "itemId = [" + item.getId() + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getNextInQueue(item))) {
            List<FeedItem> list = extractItemlistFromCursor(cursor);
            if (!list.isEmpty()) {
                FeedItem nextItem = list.get(0);
                loadAdditionalFeedItemListData(list);
                return nextItem;
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static List<FeedItem> getPausedQueue(int limit) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getPausedQueueCursor(limit))) {
            List<FeedItem> items = extractItemlistFromCursor(cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a specific FeedItem from the database.
     *
     * @param guid feed item guid
     * @param episodeUrl the feed item's url
     * @return The FeedItem or null if the FeedItem could not be found.
     *          Does NOT load additional attributes like feed or queue state.
     */
    public static FeedItem getFeedItemByGuidOrEpisodeUrl(final String guid, final String episodeUrl) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor cursor = new FeedItemCursor(adapter.getFeedItemCursor(guid, episodeUrl))) {
            List<FeedItem> list = extractItemlistFromCursor(cursor);
            if (!list.isEmpty()) {
                return list.get(0);
            }
            return null;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads shownotes information about a FeedItem.
     *
     * @param item The FeedItem
     */
    public static void loadDescriptionOfFeedItem(final FeedItem item) {
        Log.d(TAG, "loadDescriptionOfFeedItem() called with: " + "item = [" + item + "]");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getDescriptionOfItem(item)) {
            if (cursor.moveToFirst()) {
                int indexDescription = cursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION);
                String description = cursor.getString(indexDescription);
                item.setDescriptionIfLonger(description);
            }
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the list of chapters that belongs to this FeedItem if available. This method overwrites
     * any chapters that this FeedItem has. If no chapters were found in the database, the chapters
     * reference of the FeedItem will be set to null.
     *
     * @param item The FeedItem
     */
    public static List<Chapter> loadChaptersOfFeedItem(final FeedItem item) {
        Log.d(TAG, "loadChaptersOfFeedItem() called with: " + "item = [" + item + "]");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (ChapterCursor cursor = new ChapterCursor(adapter.getSimpleChaptersOfFeedItemCursor(item))) {
            int chaptersCount = cursor.getCount();
            if (chaptersCount == 0) {
                item.setChapters(null);
                return null;
            }
            ArrayList<Chapter> chapters = new ArrayList<>();
            while (cursor.moveToNext()) {
                chapters.add(cursor.getChapter());
            }
            return chapters;
        } finally {
            adapter.close();
        }
    }

    /**
     * Searches the DB for a FeedMedia of the given id.
     *
     * @param mediaId The id of the object
     * @return The found object, or null if it does not exist
     */
    @Nullable
    public static FeedMedia getFeedMedia(final long mediaId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        try (FeedItemCursor itemCursor = new FeedItemCursor(adapter.getFeedItemFromMediaIdCursor(mediaId))) {
            if (!itemCursor.moveToFirst()) {
                return null;
            }
            FeedItem item = itemCursor.getFeedItem();
            loadAdditionalFeedItemListData(Collections.singletonList(item));
            return item.getMedia();
        } finally {
            adapter.close();
        }
    }

    public static List<FeedItem> getFeedItemsWithUrl(List<String> urls) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor itemCursor = new FeedItemCursor(adapter.getFeedItemCursorByUrl(urls))) {
            List<FeedItem> items = extractItemlistFromCursor(itemCursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    public static class MonthlyStatisticsItem {
        private int year = 0;
        private int month = 0;
        private long timePlayed = 0;

        public int getYear() {
            return year;
        }

        public void setYear(final int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(final int month) {
            this.month = month;
        }

        public long getTimePlayed() {
            return timePlayed;
        }

        public void setTimePlayed(final long timePlayed) {
            this.timePlayed = timePlayed;
        }
    }

    @NonNull
    public static List<MonthlyStatisticsItem> getMonthlyTimeStatistics() {
        List<MonthlyStatisticsItem> months = new ArrayList<>();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getMonthlyStatisticsCursor()) {
            int indexMonth = cursor.getColumnIndexOrThrow("month");
            int indexYear = cursor.getColumnIndexOrThrow("year");
            int indexTotalDuration = cursor.getColumnIndexOrThrow("total_duration");
            while (cursor.moveToNext()) {
                MonthlyStatisticsItem item = new MonthlyStatisticsItem();
                item.setMonth(Integer.parseInt(cursor.getString(indexMonth)));
                item.setYear(Integer.parseInt(cursor.getString(indexYear)));
                item.setTimePlayed(cursor.getLong(indexTotalDuration));
                months.add(item);
            }
        }
        adapter.close();
        return months;
    }

    public static class StatisticsResult {
        public List<StatisticsItem> feedTime = new ArrayList<>();
        public long oldestDate = System.currentTimeMillis();
    }

    /**
     * Searches the DB for statistics.
     *
     * @return The list of statistics objects
     */
    @NonNull
    public static StatisticsResult getStatistics(boolean includeMarkedAsPlayed,
                                                 long timeFilterFrom, long timeFilterTo) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        StatisticsResult result = new StatisticsResult();
        try (FeedCursor cursor = new FeedCursor(adapter.getFeedStatisticsCursor(
                includeMarkedAsPlayed, timeFilterFrom, timeFilterTo))) {
            int indexOldestDate = cursor.getColumnIndexOrThrow("oldest_date");
            int indexNumEpisodes = cursor.getColumnIndexOrThrow("num_episodes");
            int indexEpisodesStarted = cursor.getColumnIndexOrThrow("episodes_started");
            int indexTotalTime = cursor.getColumnIndexOrThrow("total_time");
            int indexPlayedTime = cursor.getColumnIndexOrThrow("played_time");
            int indexNumDownloaded = cursor.getColumnIndexOrThrow("num_downloaded");
            int indexDownloadSize = cursor.getColumnIndexOrThrow("download_size");

            while (cursor.moveToNext()) {
                Feed feed = cursor.getFeed();

                long feedPlayedTime = cursor.getLong(indexPlayedTime) / 1000;
                long feedTotalTime = cursor.getLong(indexTotalTime) / 1000;
                long episodes = cursor.getLong(indexNumEpisodes);
                long episodesStarted = cursor.getLong(indexEpisodesStarted);
                long totalDownloadSize = cursor.getLong(indexDownloadSize);
                long episodesDownloadCount = cursor.getLong(indexNumDownloaded);
                long oldestDate = cursor.getLong(indexOldestDate);

                if (episodes > 0 && oldestDate < Long.MAX_VALUE) {
                    result.oldestDate = Math.min(result.oldestDate, oldestDate);
                }

                result.feedTime.add(new StatisticsItem(feed, feedTotalTime, feedPlayedTime, episodes,
                        episodesStarted, totalDownloadSize, episodesDownloadCount));
            }
        }
        adapter.close();
        return result;
    }

    public static long getTimeBetweenReleaseAndPlayback(long timeFilterFrom, long timeFilterTo) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getTimeBetweenReleaseAndPlayback(timeFilterFrom, timeFilterTo)) {
            cursor.moveToFirst();
            long result = cursor.getLong(0);
            adapter.close();
            return result;
        }
    }

    /**
     * Returns data necessary for displaying the navigation drawer. This includes
     * the list of subscriptions, the number of items in the queue and the number of unread
     * items.
     */
    @NonNull
    public static NavDrawerData getNavDrawerData(@Nullable SubscriptionsFilter subscriptionsFilter,
                                                 FeedOrder feedOrder, FeedCounter feedCounter) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        final Map<Long, Integer> feedCounters = adapter.getFeedCounters(feedCounter);
        List<Feed> feeds = getFeedList();

        if (subscriptionsFilter == null) {
            subscriptionsFilter = new SubscriptionsFilter("");
        }
        feeds = SubscriptionsFilterExecutor.filter(feeds, feedCounters, subscriptionsFilter);

        Comparator<Feed> comparator;
        switch (feedOrder) {
            case COUNTER:
                comparator = (lhs, rhs) -> {
                    long counterLhs = feedCounters.containsKey(lhs.getId()) ? feedCounters.get(lhs.getId()) : 0;
                    long counterRhs = feedCounters.containsKey(rhs.getId()) ? feedCounters.get(rhs.getId()) : 0;
                    if (counterLhs > counterRhs) {
                        // reverse natural order: podcast with most unplayed episodes first
                        return -1;
                    } else if (counterLhs == counterRhs) {
                        return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                    } else {
                        return 1;
                    }
                };
                break;
            case ALPHABETICAL:
                comparator = (lhs, rhs) -> {
                    String t1 = lhs.getTitle();
                    String t2 = rhs.getTitle();
                    if (t1 == null) {
                        return 1;
                    } else if (t2 == null) {
                        return -1;
                    } else {
                        return t1.compareToIgnoreCase(t2);
                    }
                };
                break;
            case MOST_PLAYED:
                final Map<Long, Integer> playedCounters = adapter.getPlayedEpisodesCounters();
                comparator = (lhs, rhs) -> {
                    long counterLhs = playedCounters.containsKey(lhs.getId()) ? playedCounters.get(lhs.getId()) : 0;
                    long counterRhs = playedCounters.containsKey(rhs.getId()) ? playedCounters.get(rhs.getId()) : 0;
                    if (counterLhs > counterRhs) {
                        // podcast with most played episodes first
                        return -1;
                    } else if (counterLhs == counterRhs) {
                        return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                    } else {
                        return 1;
                    }
                };
                break;
            default:
                final Map<Long, Long> recentPubDates = adapter.getMostRecentItemDates();
                comparator = (lhs, rhs) -> {
                    long dateLhs = recentPubDates.containsKey(lhs.getId()) ? recentPubDates.get(lhs.getId()) : 0;
                    long dateRhs = recentPubDates.containsKey(rhs.getId()) ? recentPubDates.get(rhs.getId()) : 0;
                    return Long.compare(dateRhs, dateLhs);
                };
                break;
        }

        Collections.sort(feeds, comparator);
        final int queueSize = adapter.getQueueSize();
        final int numNewItems = getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.NEW));
        final int numDownloadedItems = getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));

        List<NavDrawerData.DrawerItem> items = new ArrayList<>();
        Map<String, NavDrawerData.TagDrawerItem> folders = new HashMap<>();
        for (Feed feed : feeds) {
            for (String tag : feed.getPreferences().getTags()) {
                int counter = feedCounters.containsKey(feed.getId()) ? feedCounters.get(feed.getId()) : 0;
                NavDrawerData.FeedDrawerItem drawerItem = new NavDrawerData.FeedDrawerItem(feed, feed.getId(), counter);
                if (FeedPreferences.TAG_ROOT.equals(tag)) {
                    items.add(drawerItem);
                    continue;
                }
                NavDrawerData.TagDrawerItem folder;
                if (folders.containsKey(tag)) {
                    folder = folders.get(tag);
                } else {
                    folder = new NavDrawerData.TagDrawerItem(tag);
                    folders.put(tag, folder);
                }
                drawerItem.id |= folder.id;
                folder.getChildren().add(drawerItem);
            }
        }
        List<NavDrawerData.TagDrawerItem> foldersSorted = new ArrayList<>(folders.values());
        Collections.sort(foldersSorted, (o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
        items.addAll(foldersSorted);

        NavDrawerData result = new NavDrawerData(items, queueSize, numNewItems, numDownloadedItems, feedCounters);
        adapter.close();
        return result;
    }

    public static List<FeedItem> searchFeedItems(final long feedId, final String query) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedItemCursor searchResult = new FeedItemCursor(adapter.searchItems(feedId, query))) {
            List<FeedItem> items = extractItemlistFromCursor(searchResult);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    public static List<Feed> searchFeeds(final String query) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try (FeedCursor cursor = new FeedCursor(adapter.searchFeeds(query))) {
            List<Feed> items = new ArrayList<>();
            while (cursor.moveToNext()) {
                items.add(cursor.getFeed());
            }
            return items;
        } finally {
            adapter.close();
        }
    }
}
