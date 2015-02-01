package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.core.asynctask.FlattrStatusFetcher;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.GpodnetSyncService;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.QueueAccess;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.exception.MediaFileNotFoundException;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;

/**
 * Provides methods for doing common tasks that use DBReader and DBWriter.
 */
public final class DBTasks {
    private static final String TAG = "DBTasks";

    /**
     * Executor service used by the autodownloadUndownloadedEpisodes method.
     */
    private static ExecutorService autodownloadExec;

    static {
        autodownloadExec = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    private DBTasks() {
    }

    /**
     * Removes the feed with the given download url. This method should NOT be executed on the GUI thread.
     *
     * @param context     Used for accessing the db
     * @param downloadUrl URL of the feed.
     */
    public static void removeFeedWithDownloadUrl(Context context, String downloadUrl) {
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getFeedCursorDownloadUrls();
        long feedID = 0;
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(1).equals(downloadUrl)) {
                    feedID = cursor.getLong(0);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.close();

        if (feedID != 0) {
            try {
                DBWriter.deleteFeed(context, feedID).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "removeFeedWithDownloadUrl: Could not find feed with url: " + downloadUrl);
        }
    }

    /**
     * Starts playback of a FeedMedia object's file. This method will build an Intent based on the given parameters to
     * start the {@link PlaybackService}.
     *
     * @param context           Used for sending starting Services and Activities.
     * @param media             The FeedMedia object.
     * @param showPlayer        If true, starts the appropriate player activity ({@link de.danoeh.antennapod.activity.AudioplayerActivity}
     *                          or {@link de.danoeh.antennapod.activity.VideoplayerActivity}
     * @param startWhenPrepared Parameter for the {@link PlaybackService} start intent. If true, playback will start as
     *                          soon as the PlaybackService has finished loading the FeedMedia object's file.
     * @param shouldStream      Parameter for the {@link PlaybackService} start intent. If true, the FeedMedia object's file
     *                          will be streamed, otherwise the downloaded file will be used. If the downloaded file cannot be
     *                          found, the PlaybackService will shutdown and the database entry of the FeedMedia object will be
     *                          corrected.
     */
    public static void playMedia(final Context context, final FeedMedia media,
                                 boolean showPlayer, boolean startWhenPrepared, boolean shouldStream) {
        try {
            if (!shouldStream) {
                if (media.fileExists() == false) {
                    throw new MediaFileNotFoundException(
                            "No episode was found at " + media.getFile_url(),
                            media);
                }
            }
            // Start playback Service
            Intent launchIntent = new Intent(context, PlaybackService.class);
            launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
            launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
                    startWhenPrepared);
            launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
                    shouldStream);
            launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
                    true);
            context.startService(launchIntent);
            if (showPlayer) {
                // Launch media player
                context.startActivity(PlaybackService.getPlayerActivityIntent(
                        context, media));
            }
            DBWriter.addQueueItemAt(context, media.getItem().getId(), 0, false);
        } catch (MediaFileNotFoundException e) {
            e.printStackTrace();
            if (media.isPlaying()) {
                context.sendBroadcast(new Intent(
                        PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
            }
            notifyMissingFeedMediaFile(context, media);
        }
    }

    private static AtomicBoolean isRefreshing = new AtomicBoolean(false);

    /**
     * Refreshes a given list of Feeds in a separate Thread. This method might ignore subsequent calls if it is still
     * enqueuing Feeds for download from a previous call
     *
     * @param context Might be used for accessing the database
     * @param feeds   List of Feeds that should be refreshed.
     */
    public static void refreshAllFeeds(final Context context,
                                       final List<Feed> feeds) {
        if (isRefreshing.compareAndSet(false, true)) {
            new Thread() {
                public void run() {
                    if (feeds != null) {
                        refreshFeeds(context, feeds);
                    } else {
                        refreshFeeds(context, DBReader.getFeedList(context));
                    }
                    isRefreshing.set(false);

                    if (FlattrUtils.hasToken()) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Flattring all pending things.");
                        new FlattrClickWorker(context).executeAsync(); // flattr pending things

                        if (BuildConfig.DEBUG) Log.d(TAG, "Fetching flattr status.");
                        new FlattrStatusFetcher(context).start();

                    }
                    if (ClientConfig.gpodnetCallbacks.gpodnetEnabled()) {
                        GpodnetSyncService.sendSyncIntent(context);
                    }
                    autodownloadUndownloadedItems(context);
                }
            }.start();
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG,
                        "Ignoring request to refresh all feeds: Refresh lock is locked");
        }
    }

    /**
     * Used by refreshExpiredFeeds to determine which feeds should be refreshed.
     * This method will use the value specified in the UserPreferences as the
     * expiration time.
     *
     * @param context Used for DB access.
     * @return A list of expired feeds. An empty list will be returned if there
     * are no expired feeds.
     */
    public static List<Feed> getExpiredFeeds(final Context context) {
        long millis = UserPreferences.getUpdateInterval();

        if (millis > 0) {

            List<Feed> feedList = DBReader.getExpiredFeedsList(context,
                    millis);
            if (feedList.size() > 0) {
                refreshFeeds(context, feedList);
            }
            return feedList;
        } else {
            return new ArrayList<Feed>();
        }
    }

    /**
     * Refreshes expired Feeds in the list returned by the getExpiredFeedsList(Context, long) method in DBReader.
     * The expiration date parameter is determined by the update interval specified in {@link UserPreferences}.
     *
     * @param context Used for DB access.
     */
    public static void refreshExpiredFeeds(final Context context) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Refreshing expired feeds");

        new Thread() {
            public void run() {
                refreshFeeds(context, getExpiredFeeds(context));
            }
        }.start();
    }

    private static void refreshFeeds(final Context context,
                                     final List<Feed> feedList) {

        for (Feed feed : feedList) {
            try {
                refreshFeed(context, feed);
            } catch (DownloadRequestException e) {
                e.printStackTrace();
                DBWriter.addDownloadStatus(
                        context,
                        new DownloadStatus(feed, feed
                                .getHumanReadableIdentifier(),
                                DownloadError.ERROR_REQUEST_ERROR, false, e
                                .getMessage()
                        )
                );
            }
        }

    }

    /**
     * Downloads all pages of the given feed.
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    public static void refreshCompleteFeed(final Context context, final Feed feed) {
        try {
            refreshFeed(context, feed, true);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DBWriter.addDownloadStatus(
                    context,
                    new DownloadStatus(feed, feed
                            .getHumanReadableIdentifier(),
                            DownloadError.ERROR_REQUEST_ERROR, false, e
                            .getMessage()
                    )
            );
        }
    }

    /**
     * Queues the next page of this Feed for download. The given Feed has to be a paged
     * Feed (isPaged()=true) and must contain a nextPageLink.
     *
     * @param context      Used for requesting the download.
     * @param feed         The feed whose next page should be loaded.
     * @param loadAllPages True if any subsequent pages should also be loaded, false otherwise.
     */
    public static void loadNextPageOfFeed(final Context context, Feed feed, boolean loadAllPages) throws DownloadRequestException {
        if (feed.isPaged() && feed.getNextPageLink() != null) {
            int pageNr = feed.getPageNr() + 1;
            Feed nextFeed = new Feed(feed.getNextPageLink(), new Date(), feed.getTitle() + "(" + pageNr + ")");
            nextFeed.setPageNr(pageNr);
            nextFeed.setPaged(true);
            nextFeed.setId(feed.getId());
            DownloadRequester.getInstance().downloadFeed(context, nextFeed, loadAllPages);
        } else {
            Log.e(TAG, "loadNextPageOfFeed: Feed was either not paged or contained no nextPageLink");
        }
    }

    /**
     * Updates a specific Feed.
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    public static void refreshFeed(Context context, Feed feed)
            throws DownloadRequestException {
        refreshFeed(context, feed, false);
    }

    private static void refreshFeed(Context context, Feed feed, boolean loadAllPages) throws DownloadRequestException {
        Feed f;
        if (feed.getPreferences() == null) {
            f = new Feed(feed.getDownload_url(), new Date(), feed.getTitle());
        } else {
            f = new Feed(feed.getDownload_url(), new Date(), feed.getTitle(),
                    feed.getPreferences().getUsername(), feed.getPreferences().getPassword());
        }
        f.setId(feed.getId());
        DownloadRequester.getInstance().downloadFeed(context, f, loadAllPages);
    }

    /**
     * Notifies the database about a missing FeedImage file. This method will attempt to re-download the file.
     *
     * @param context Used for requesting the download.
     * @param image   The FeedImage object.
     */
    public static void notifyInvalidImageFile(final Context context,
                                              final FeedImage image) {
        Log.i(TAG,
                "The DB was notified about an invalid image download. It will now try to re-download the image file");
        try {
            DownloadRequester.getInstance().downloadImage(context, image);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            Log.w(TAG, "Failed to download invalid feed image");
        }
    }

    /**
     * Notifies the database about a missing FeedMedia file. This method will correct the FeedMedia object's values in the
     * DB and send a FeedUpdateBroadcast.
     */
    public static void notifyMissingFeedMediaFile(final Context context,
                                                  final FeedMedia media) {
        Log.i(TAG,
                "The feedmanager was notified about a missing episode. It will update its database now.");
        media.setDownloaded(false);
        media.setFile_url(null);
        DBWriter.setFeedMedia(context, media);
        EventDistributor.getInstance().sendFeedUpdateBroadcast();
    }

    /**
     * Request the download of all objects in the queue. from a separate Thread.
     *
     * @param context Used for requesting the download an accessing the database.
     */
    public static void downloadAllItemsInQueue(final Context context) {
        new Thread() {
            public void run() {
                List<FeedItem> queue = DBReader.getQueue(context);
                if (!queue.isEmpty()) {
                    try {
                        downloadFeedItems(context,
                                queue.toArray(new FeedItem[queue.size()]));
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Requests the download of a list of FeedItem objects.
     *
     * @param context Used for requesting the download and accessing the DB.
     * @param items   The FeedItem objects.
     */
    public static void downloadFeedItems(final Context context,
                                         FeedItem... items) throws DownloadRequestException {
        downloadFeedItems(true, context, items);
    }

    static void downloadFeedItems(boolean performAutoCleanup,
                                  final Context context, final FeedItem... items)
            throws DownloadRequestException {
        final DownloadRequester requester = DownloadRequester.getInstance();

        if (performAutoCleanup) {
            new Thread() {

                @Override
                public void run() {
                    ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm()
                            .performCleanup(context,
                                    ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm()
                                            .getPerformCleanupParameter(context, Arrays.asList(items)));
                }

            }.start();
        }
        for (FeedItem item : items) {
            if (item.getMedia() != null
                    && !requester.isDownloadingFile(item.getMedia())
                    && !item.getMedia().isDownloaded()) {
                if (items.length > 1) {
                    try {
                        requester.downloadMedia(context, item.getMedia());
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                        DBWriter.addDownloadStatus(context,
                                new DownloadStatus(item.getMedia(), item
                                        .getMedia()
                                        .getHumanReadableIdentifier(),
                                        DownloadError.ERROR_REQUEST_ERROR,
                                        false, e.getMessage()
                                )
                        );
                    }
                } else {
                    requester.downloadMedia(context, item.getMedia());
                }
            }
        }
    }

    static int getNumberOfUndownloadedEpisodes(
            final List<FeedItem> queue, final List<FeedItem> unreadItems) {
        int counter = 0;
        for (FeedItem item : queue) {
            if (item.hasMedia() && !item.getMedia().isDownloaded()
                    && !item.getMedia().isPlaying()
                    && item.getFeed().getPreferences().getAutoDownload()) {
                counter++;
            }
        }
        for (FeedItem item : unreadItems) {
            if (item.hasMedia() && !item.getMedia().isDownloaded()
                    && item.getFeed().getPreferences().getAutoDownload()) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Looks for undownloaded episodes in the queue or list of unread items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @param mediaIds If this list is not empty, the method will only download a candidate for automatic downloading if
     *                 its media ID is in the mediaIds list.
     * @return A Future that can be used for waiting for the methods completion.
     */
    public static Future<?> autodownloadUndownloadedItems(final Context context, final long... mediaIds) {
        return autodownloadExec.submit(ClientConfig.dbTasksCallbacks.getAutomaticDownloadAlgorithm()
                .autoDownloadUndownloadedItems(context, mediaIds));

    }

    /**
     * Removed downloaded episodes outside of the queue if the episode cache is full. Episodes with a smaller
     * 'playbackCompletionDate'-value will be deleted first.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     *
     * @param context Used for accessing the DB.
     */
    public static void performAutoCleanup(final Context context) {
        ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm().performCleanup(context,
                ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm().getDefaultCleanupParameter(context));
    }

    /**
     * Adds all FeedItem objects whose 'read'-attribute is false to the queue in a separate thread.
     */
    public static void enqueueAllNewItems(final Context context) {
        long[] unreadItems = DBReader.getUnreadItemIds(context);
        DBWriter.addQueueItem(context, unreadItems);
    }

    /**
     * Returns the successor of a FeedItem in the queue.
     *
     * @param context Used for accessing the DB.
     * @param itemId  ID of the FeedItem
     * @param queue   Used for determining the successor of the item. If this parameter is null, the method will load
     *                the queue from the database in the same thread.
     * @return Successor of the FeedItem or null if the FeedItem is not in the queue or has no successor.
     */
    public static FeedItem getQueueSuccessorOfItem(Context context,
                                                   final long itemId, List<FeedItem> queue) {
        FeedItem result = null;
        if (queue == null) {
            queue = DBReader.getQueue(context);
        }
        if (queue != null) {
            Iterator<FeedItem> iterator = queue.iterator();
            while (iterator.hasNext()) {
                FeedItem item = iterator.next();
                if (item.getId() == itemId) {
                    if (iterator.hasNext()) {
                        result = iterator.next();
                    }
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Loads the queue from the database and checks if the specified FeedItem is in the queue.
     * This method should NOT be executed in the GUI thread.
     *
     * @param context    Used for accessing the DB.
     * @param feedItemId ID of the FeedItem
     */
    public static boolean isInQueue(Context context, final long feedItemId) {
        List<Long> queue = DBReader.getQueueIDList(context);
        return QueueAccess.IDListAccess(queue).contains(feedItemId);
    }

    private static Feed searchFeedByIdentifyingValueOrID(Context context, PodDBAdapter adapter,
                                                         Feed feed) {
        if (feed.getId() != 0) {
            return DBReader.getFeed(context, feed.getId(), adapter);
        } else {
            List<Feed> feeds = DBReader.getFeedList(context);
            for (Feed f : feeds) {
                if (f.getIdentifyingValue().equals(feed.getIdentifyingValue())) {
                    f.setItems(DBReader.getFeedItemList(context, f));
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Get a FeedItem by its identifying value.
     */
    private static FeedItem searchFeedItemByIdentifyingValue(Feed feed,
                                                             String identifier) {
        for (FeedItem item : feed.getItems()) {
            if (item.getIdentifyingValue().equals(identifier)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Adds new Feeds to the database or updates the old versions if they already exists. If another Feed with the same
     * identifying value already exists, this method will add new FeedItems from the new Feed to the existing Feed.
     * These FeedItems will be marked as unread.
     * <p/>
     * This method can update multiple feeds at once. Submitting a feed twice in the same method call can result in undefined behavior.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     *
     * @param context  Used for accessing the DB.
     * @param newFeeds The new Feed objects.
     * @return The updated Feeds from the database if it already existed, or the new Feed from the parameters otherwise.
     */
    public static synchronized Feed[] updateFeed(final Context context,
                                                 final Feed... newFeeds) {
        List<Feed> newFeedsList = new ArrayList<Feed>();
        List<Feed> updatedFeedsList = new ArrayList<Feed>();
        Feed[] resultFeeds = new Feed[newFeeds.length];
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        for (int feedIdx = 0; feedIdx < newFeeds.length; feedIdx++) {

            final Feed newFeed = newFeeds[feedIdx];

            // Look up feed in the feedslist
            final Feed savedFeed = searchFeedByIdentifyingValueOrID(context, adapter,
                    newFeed);
            if (savedFeed == null) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG,
                            "Found no existing Feed with title "
                                    + newFeed.getTitle() + ". Adding as new one."
                    );
                // Add a new Feed
                newFeedsList.add(newFeed);
                resultFeeds[feedIdx] = newFeed;
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Feed with title " + newFeed.getTitle()
                            + " already exists. Syncing new with existing one.");

                Collections.sort(newFeed.getItems(), new FeedItemPubdateComparator());

                final boolean markNewItemsAsUnread;
                if (newFeed.getPageNr() == savedFeed.getPageNr()) {
                    if (savedFeed.compareWithOther(newFeed)) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG,
                                    "Feed has updated attribute values. Updating old feed's attributes");
                        savedFeed.updateFromOther(newFeed);
                    }
                    markNewItemsAsUnread = true;
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "New feed has a higher page number. Merging without marking as unread");
                    markNewItemsAsUnread = false;
                    savedFeed.setNextPageLink(newFeed.getNextPageLink());
                }
                if (savedFeed.getPreferences().compareWithOther(newFeed.getPreferences())) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Feed has updated preferences. Updating old feed's preferences");
                    savedFeed.getPreferences().updateFromOther(newFeed.getPreferences());
                }
                // Look for new or updated Items
                for (int idx = 0; idx < newFeed.getItems().size(); idx++) {
                    final FeedItem item = newFeed.getItems().get(idx);
                    FeedItem oldItem = searchFeedItemByIdentifyingValue(savedFeed,
                            item.getIdentifyingValue());
                    if (oldItem == null) {
                        // item is new
                        final int i = idx;
                        item.setFeed(savedFeed);
                        savedFeed.getItems().add(i, item);
                        if (markNewItemsAsUnread) {
                            item.setRead(false);
                        }
                    } else {
                        oldItem.updateFromOther(item);
                    }
                }
                // update attributes
                savedFeed.setLastUpdate(newFeed.getLastUpdate());
                savedFeed.setType(newFeed.getType());

                updatedFeedsList.add(savedFeed);
                resultFeeds[feedIdx] = savedFeed;
            }
        }

        adapter.close();

        try {
            DBWriter.addNewFeed(context, newFeedsList.toArray(new Feed[newFeedsList.size()])).get();
            DBWriter.setCompleteFeed(context, updatedFeedsList.toArray(new Feed[updatedFeedsList.size()])).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        EventDistributor.getInstance().sendFeedUpdateBroadcast();

        return resultFeeds;
    }

    /**
     * Searches the titles of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string.
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemTitle(final Context context,
                                                                 final long feedID, final String query) {
        return new FutureTask<List<FeedItem>>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemTitles(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(context, searchResult);
                DBReader.loadFeedDataOfFeedItemlist(context, items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the descriptions of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemDescription(final Context context,
                                                                       final long feedID, final String query) {
        return new FutureTask<List<FeedItem>>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemDescriptions(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(context, searchResult);
                DBReader.loadFeedDataOfFeedItemlist(context, items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the contentEncoded-value of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemContentEncoded(final Context context,
                                                                          final long feedID, final String query) {
        return new FutureTask<List<FeedItem>>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemContentEncoded(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(context, searchResult);
                DBReader.loadFeedDataOfFeedItemlist(context, items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches chapters of the FeedItems of a specific Feed for a given string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemChapters(final Context context,
                                                                    final long feedID, final String query) {
        return new FutureTask<List<FeedItem>>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemChapters(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(context, searchResult);
                DBReader.loadFeedDataOfFeedItemlist(context, items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * A runnable which should be used for database queries. The onCompletion
     * method is executed on the database executor to handle Cursors correctly.
     * This class automatically creates a PodDBAdapter object and closes it when
     * it is no longer in use.
     */
    static abstract class QueryTask<T> implements Callable<T> {
        private T result;
        private Context context;

        public QueryTask(Context context) {
            this.context = context;
        }

        @Override
        public T call() throws Exception {
            PodDBAdapter adapter = new PodDBAdapter(context);
            adapter.open();
            execute(adapter);
            adapter.close();
            return result;
        }

        public abstract void execute(PodDBAdapter adapter);

        protected void setResult(T result) {
            this.result = result;
        }
    }

    /**
     * Adds the given FeedItem to the flattr queue if the user is logged in. Otherwise, a dialog
     * will be opened that lets the user go either to the login screen or the website of the flattr thing.
     *
     * @param context
     * @param item
     */
    public static void flattrItemIfLoggedIn(Context context, FeedItem item) {
        if (FlattrUtils.hasToken()) {
            item.getFlattrStatus().setFlattrQueue();
            DBWriter.setFlattredStatus(context, item, true);
        } else {
            FlattrUtils.showNoTokenDialogOrRedirect(context, item.getPaymentLink());
        }
    }

    /**
     * Adds the given Feed to the flattr queue if the user is logged in. Otherwise, a dialog
     * will be opened that lets the user go either to the login screen or the website of the flattr thing.
     *
     * @param context
     * @param feed
     */
    public static void flattrFeedIfLoggedIn(Context context, Feed feed) {
        if (FlattrUtils.hasToken()) {
            feed.getFlattrStatus().setFlattrQueue();
            DBWriter.setFlattredStatus(context, feed, true);
        } else {
            FlattrUtils.showNoTokenDialogOrRedirect(context, feed.getPaymentLink());
        }
    }

}
